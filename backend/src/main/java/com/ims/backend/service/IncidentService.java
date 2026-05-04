package com.ims.backend.service;

import com.ims.backend.dto.IncidentResponse;
import com.ims.backend.dto.StatusTransitionRequest;
import com.ims.backend.exception.ResourceNotFoundException;
import com.ims.backend.model.Incident;
import com.ims.backend.model.IncidentStatus;
import com.ims.backend.repository.relational.IncidentRepository;
import com.ims.backend.repository.relational.RcaRepository;
import com.ims.backend.state.IncidentState;
import com.ims.backend.state.IncidentStateFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Core service for incident lifecycle management.
 *
 * Applies State Pattern for status transitions.
 * Uses Redis as L1 cache (cache-aside pattern) with PostgreSQL as source of truth.
 * Circuit breakers wrap DB calls to prevent cascade failures.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final RcaRepository      rcaRepository;
    private final ReactiveRedisTemplate<String, Incident> redisTemplate;

    @Value("${ims.cache.incident-ttl-minutes:60}")
    private long cacheTtlMinutes;

    private static final String CACHE_KEY_PREFIX = "incident:";

    // ─────────────────────────────────────────────
    //  Queries
    // ─────────────────────────────────────────────

    @CircuitBreaker(name = "mysqlDB", fallbackMethod = "getIncidentFallback")
    @Retry(name = "mysqlDB")
    public Mono<IncidentResponse> getIncident(String id) {
        String cacheKey = CACHE_KEY_PREFIX + id;

        return redisTemplate.opsForValue()
                .get(cacheKey)
                .doOnNext(i -> log.debug("Cache HIT for incident {}", id))
                .switchIfEmpty(
                        incidentRepository.findById(id)
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Incident", id)))
                                .flatMap(incident -> redisTemplate.opsForValue()
                                        .set(cacheKey, incident, Duration.ofMinutes(cacheTtlMinutes))
                                        .thenReturn(incident)
                                        .doOnSuccess(i -> log.debug("Cache MISS – loaded incident {} from DB", id)))
                )
                .map(IncidentResponse::from);
    }

    public Mono<IncidentResponse> getIncidentFallback(String id, Exception ex) {
        log.warn("Circuit breaker fallback for incident {}: {}", id, ex.getMessage());
        // Try Redis-only on DB failure
        return redisTemplate.opsForValue()
                .get(CACHE_KEY_PREFIX + id)
                .map(IncidentResponse::from)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Incident", id)));
    }

    public Flux<IncidentResponse> getIncidentsByStatus(String status) {
        return incidentRepository.findByStatusOrderByStartTimeDesc(status)
                .map(IncidentResponse::from);
    }

    public Flux<IncidentResponse> getIncidentsByComponent(String componentId) {
        return incidentRepository.findByComponentIdOrderByStartTimeDesc(componentId)
                .map(IncidentResponse::from);
    }

    // ─────────────────────────────────────────────
    //  Status Transitions
    // ─────────────────────────────────────────────

    /**
     * Transitions an incident to a new status using the State Pattern.
     * Atomically updates PostgreSQL and invalidates Redis cache.
     * Thread-safe: optimistic locking via @Transactional + DB-level upsert.
     */
    @Transactional
    @CircuitBreaker(name = "mysqlDB")
    @Retry(name = "mysqlDB")
    public Mono<IncidentResponse> transitionStatus(String id, StatusTransitionRequest req) {
        return incidentRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Incident", id)))
                .flatMap(incident -> {
                    IncidentStatus target = req.getTargetStatus();

                    // For CLOSED transition, check RCA exists
                    Mono<Boolean> hasRca = (target == IncidentStatus.CLOSED)
                            ? rcaRepository.existsByIncidentId(id)
                            : Mono.just(false);

                    return hasRca.flatMap(rcaExists -> {
                        // State machine transition (throws on invalid)
                        IncidentState state = IncidentStateFactory.of(incident.getStatus());
                        IncidentState newState = state.transition(target, rcaExists);

                        Instant now = Instant.now();
                        incident.setStatus(newState.getStatus().name());
                        incident.setUpdatedAt(now);

                        // Compute MTTR when resolving
                        if (newState.getStatus() == IncidentStatus.RESOLVED) {
                            incident.setEndTime(now);
                            long mttr = Duration.between(incident.getStartTime(), now).toMinutes();
                            incident.setMttrMinutes(mttr);
                            log.info("Incident {} RESOLVED. MTTR = {} minutes", id, mttr);
                        }

                        return incidentRepository.save(incident)
                                .flatMap(saved ->
                                        redisTemplate.opsForValue()
                                                .delete(CACHE_KEY_PREFIX + id)
                                                .thenReturn(saved)
                                );
                    });
                })
                .map(IncidentResponse::from)
                .doOnSuccess(r -> log.info("Incident {} transitioned to {}", id, r.getStatus()));
    }

    // ─────────────────────────────────────────────
    //  Internal: used by SignalConsumerService
    // ─────────────────────────────────────────────

    /** Finds an active incident for a component, or empty if none. */
    public Mono<Incident> findActiveIncident(String componentId) {
        return incidentRepository.findActiveByComponentId(componentId);
    }

    /** Persists a new incident and caches it. */
    @CircuitBreaker(name = "mysqlDB")
    @Retry(name = "mysqlDB")
    public Mono<Incident> createIncident(Incident incident) {
        return incidentRepository.save(incident)
                .flatMap(saved -> {
                    String key = CACHE_KEY_PREFIX + saved.getId();
                    return redisTemplate.opsForValue()
                            .set(key, saved, Duration.ofMinutes(cacheTtlMinutes))
                            .thenReturn(saved);
                })
                .doOnSuccess(i -> log.info("New incident created: {} for component {}", i.getId(), i.getComponentId()));
    }
}
