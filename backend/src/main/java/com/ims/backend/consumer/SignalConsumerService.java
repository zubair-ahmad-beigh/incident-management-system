package com.ims.backend.consumer;

import com.ims.backend.dto.SignalEvent;
import com.ims.backend.model.Incident;
import com.ims.backend.model.Signal;
import com.ims.backend.model.Severity;
import com.ims.backend.repository.document.SignalRepository;
import com.ims.backend.service.IncidentService;
import com.ims.backend.strategy.AlertStrategyResolver;
import com.ims.backend.util.ThroughputTracker;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka consumer for signal events.
 *
 * Processing pipeline per signal:
 *  1. Persist raw signal to MongoDB
 *  2. Check if an active incident exists for the component (debounce window)
 *  3. If yes  → link signal to existing incident (no duplicate incident)
 *  4. If no   → create new incident with severity from AlertStrategy
 *
 * Acknowledgment is manual (AckMode.MANUAL_IMMEDIATE) so failed messages
 * are NOT committed – they'll be retried by Kafka consumer group.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignalConsumerService {

    private final SignalRepository     signalRepository;
    private final IncidentService      incidentService;
    private final AlertStrategyResolver strategyResolver;
    private final ThroughputTracker    throughputTracker;

    @KafkaListener(
            topics        = "${ims.kafka.topics.signals}",
            groupId       = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload SignalEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        long startNanos = System.nanoTime();
        log.debug("Consuming signal {} from partition={} offset={}", event.getSignalId(), partition, offset);

        processSignal(event)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(incidentId -> {
                    long latency = System.nanoTime() - startNanos;
                    throughputTracker.recordLatency(latency);
                    log.debug("Signal {} processed → incident {}. Latency={}ms",
                            event.getSignalId(), incidentId, latency / 1_000_000);
                    ack.acknowledge(); // commit offset only on success
                })
                .doOnError(ex -> log.error("Error processing signal {}: {}", event.getSignalId(), ex.getMessage(), ex))
                // On error, DO NOT acknowledge – message will be retried
                .subscribe();
    }

    @CircuitBreaker(name = "mongoDB")
    @Retry(name = "mongoDB")
    private Mono<String> processSignal(SignalEvent event) {
        // Step 1: persist raw signal to MongoDB
        Signal rawSignal = Signal.builder()
                .id(event.getSignalId())
                .componentId(event.getComponentId())
                .type(event.getType())
                .message(event.getMessage())
                .rawPayload(event.getRawPayload())
                .timestamp(event.getTimestamp())
                .processedAt(Instant.now())
                .build();

        return signalRepository.save(rawSignal)
                .flatMap(savedSignal ->
                        // Step 2: debounce – look up existing active incident
                        incidentService.findActiveIncident(event.getComponentId())
                                .flatMap(existingIncident -> {
                                    // Link signal to existing incident (no new incident created)
                                    log.debug("Debounce: linking signal {} to existing incident {}",
                                            event.getSignalId(), existingIncident.getId());
                                    savedSignal.setIncidentId(existingIncident.getId().toString());
                                    return signalRepository.save(savedSignal)
                                            .thenReturn(existingIncident.getId());
                                })
                                .switchIfEmpty(
                                        // Step 3: no active incident – create one
                                        createNewIncident(event, savedSignal)
                                )
                );
    }

    private Mono<String> createNewIncident(SignalEvent event, Signal signal) {
        Severity severity = strategyResolver.resolve(event.getType(), event.getComponentId());
        Instant now = Instant.now();

        Incident newIncident = Incident.builder()
                .id(UUID.randomUUID().toString())
                .componentId(event.getComponentId())
                .severity(severity.name())
                .startTime(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return incidentService.createIncident(newIncident)
                .flatMap(savedIncident -> {
                    signal.setIncidentId(savedIncident.getId().toString());
                    return signalRepository.save(signal)
                            .thenReturn(savedIncident.getId());
                });
    }
}
