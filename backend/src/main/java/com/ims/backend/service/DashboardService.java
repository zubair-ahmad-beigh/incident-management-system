package com.ims.backend.service;

import com.ims.backend.model.Incident;
import com.ims.backend.repository.document.SignalRepository;
import com.ims.backend.repository.relational.IncidentRepository;
import com.ims.backend.repository.relational.RcaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Dashboard service – aggregates metrics from MySQL, MongoDB, and Redis
 * for the operational overview API.
 *
 * GET /api/dashboard/summary
 */
@Service
public class DashboardService {

    private final IncidentRepository incidentRepository;
    private final SignalRepository   signalRepository;
    private final RcaRepository      rcaRepository;
    private final ReactiveRedisTemplate<String, Incident> redisTemplate;

    public DashboardService(
            IncidentRepository incidentRepository,
            SignalRepository signalRepository,
            RcaRepository rcaRepository,
            @Qualifier("incidentRedisTemplate") ReactiveRedisTemplate<String, Incident> redisTemplate) {
        this.incidentRepository = incidentRepository;
        this.signalRepository   = signalRepository;
        this.rcaRepository      = rcaRepository;
        this.redisTemplate      = redisTemplate;
    }

    /**
     * Returns a real-time summary map:
     * {
     *   openIncidents:         long,
     *   investigatingIncidents: long,
     *   resolvedIncidents:     long,
     *   closedIncidents:       long,
     *   totalSignals:          long,
     *   redisKeys:             long
     * }
     */
    public Mono<Map<String, Object>> getSummary() {
        Mono<Long> openCount         = incidentRepository.findByStatusOrderByStartTimeDesc("OPEN").count();
        Mono<Long> investigatingCount = incidentRepository.findByStatusOrderByStartTimeDesc("INVESTIGATING").count();
        Mono<Long> resolvedCount     = incidentRepository.findByStatusOrderByStartTimeDesc("RESOLVED").count();
        Mono<Long> closedCount       = incidentRepository.findByStatusOrderByStartTimeDesc("CLOSED").count();
        Mono<Long> signalTotal       = signalRepository.count();
        Mono<Long> redisKeys         = redisTemplate.scan(
                ScanOptions.scanOptions().match("incident:*").count(1000).build()
        ).count();

        return Mono.zip(openCount, investigatingCount, resolvedCount, closedCount, signalTotal, redisKeys)
                .map(t -> Map.of(
                        "openIncidents",          t.getT1(),
                        "investigatingIncidents", t.getT2(),
                        "resolvedIncidents",      t.getT3(),
                        "closedIncidents",        t.getT4(),
                        "totalSignalsIngested",   t.getT5(),
                        "incidentsCachedInRedis", t.getT6()
                ));
    }
}
