package com.ims.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Throughput tracker that logs signals/sec every 5 seconds.
 * Uses lock-free AtomicLong – safe for high-concurrency ingestion paths.
 */
@Slf4j
@Component
public class ThroughputTracker {

    private final AtomicLong signalCount    = new AtomicLong(0);
    private final AtomicLong processingTime = new AtomicLong(0); // nanos

    /** Called each time a signal is accepted into the ingestion pipeline. */
    public void recordSignal() {
        signalCount.incrementAndGet();
    }

    /**
     * Records consumer-side processing latency.
     *
     * @param latencyNanos elapsed nano-seconds from signal receipt to DB write
     */
    public void recordLatency(long latencyNanos) {
        processingTime.addAndGet(latencyNanos);
    }

    /** Scheduled log every 5 seconds – resets counters after logging. */
    @Scheduled(fixedDelay = 5000)
    public void logThroughput() {
        long count   = signalCount.getAndSet(0);
        long latency = processingTime.getAndSet(0);

        double signalsPerSec = count / 5.0;
        double avgLatencyMs  = count > 0 ? (latency / (double) count) / 1_000_000.0 : 0;

        log.info("[METRICS] Throughput: {} signals/sec | AvgLatency: {} ms | Total in 5s: {}",
                String.format("%.1f", signalsPerSec),
                String.format("%.2f", avgLatencyMs),
                count);
    }
}
