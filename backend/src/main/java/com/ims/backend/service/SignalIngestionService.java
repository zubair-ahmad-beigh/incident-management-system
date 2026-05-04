package com.ims.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.backend.dto.SignalEvent;
import com.ims.backend.dto.SignalRequest;
import com.ims.backend.exception.RateLimitExceededException;
import com.ims.backend.util.RateLimiter;
import com.ims.backend.util.ThroughputTracker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

/**
 * Signal ingestion service.
 *
 * Hot path: validate → rate-limit → serialize → push to Kafka.
 * No blocking DB operations in this path.
 */
@Slf4j
@Service
public class SignalIngestionService {

    private final KafkaTemplate<String, SignalEvent> kafkaTemplate;
    private final RateLimiter rateLimiter;
    private final ThroughputTracker throughputTracker;
    private final ObjectMapper objectMapper;
    private final Counter signalAcceptedCounter;
    private final Counter signalRejectedCounter;

    @Value("${ims.kafka.topics.signals}")
    private String signalsTopic;

    public SignalIngestionService(
            KafkaTemplate<String, SignalEvent> kafkaTemplate,
            RateLimiter rateLimiter,
            ThroughputTracker throughputTracker,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.kafkaTemplate       = kafkaTemplate;
        this.rateLimiter         = rateLimiter;
        this.throughputTracker   = throughputTracker;
        this.objectMapper        = objectMapper;
        this.signalAcceptedCounter = meterRegistry.counter("ims.signals.accepted");
        this.signalRejectedCounter = meterRegistry.counter("ims.signals.rejected");
    }

    /**
     * Ingests a signal:
     *  1. Apply rate limiting (per remote IP)
     *  2. Build Kafka event
     *  3. Publish to Kafka asynchronously (non-blocking via subscribeOn boundedElastic)
     *
     * @param request   validated signal payload
     * @param remoteIp  caller's IP for rate-limiting key
     * @return          signal ID string on success
     */
    public Mono<String> ingest(SignalRequest request, String remoteIp) {
        return Mono.fromCallable(() -> {
                    // Rate limit check – fast, in-memory
                    rateLimiter.consume(remoteIp);

                    String signalId = UUID.randomUUID().toString();
                    String rawPayload = toJson(request);

                    SignalEvent event = SignalEvent.builder()
                            .signalId(signalId)
                            .componentId(request.getComponentId())
                            .type(request.getType())
                            .message(request.getMessage())
                            .rawPayload(rawPayload)
                            .timestamp(request.getTimestamp())
                            .build();

                    // Publish to Kafka – componentId as partition key for ordering
                    kafkaTemplate.send(signalsTopic, request.getComponentId(), event)
                            .whenComplete((result, ex) -> {
                                if (ex != null) {
                                    log.error("Failed to publish signal {} to Kafka: {}", signalId, ex.getMessage());
                                    signalRejectedCounter.increment();
                                } else {
                                    log.debug("Signal {} published to partition {}",
                                            signalId,
                                            result.getRecordMetadata().partition());
                                }
                            });

                    throughputTracker.recordSignal();
                    signalAcceptedCounter.increment();
                    return signalId;
                })
                .subscribeOn(Schedulers.boundedElastic()) // offload blocking Kafka I/O
                .onErrorMap(RateLimitExceededException.class, e -> e)
                .doOnError(ex -> {
                    if (!(ex instanceof RateLimitExceededException)) {
                        log.error("Signal ingestion error", ex);
                    }
                });
    }

    private String toJson(SignalRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
