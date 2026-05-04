package com.ims.backend.util;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom Micrometer metrics for Prometheus/Grafana dashboards.
 *
 * Exposes:
 *  - ims.signals.ingested.total      (counter)
 *  - ims.incidents.active.count      (gauge)
 *
 * Registered in MeterRegistry so Prometheus scrapes them at /actuator/prometheus.
 */
@Component
@RequiredArgsConstructor
public class MetricsRegistry {

    private final AtomicLong activeIncidentCount = new AtomicLong(0);

    public MetricsRegistry(MeterRegistry registry) {
        Gauge.builder("ims.incidents.active.count", activeIncidentCount, AtomicLong::get)
                .description("Number of currently active (non-CLOSED) incidents")
                .register(registry);
    }

    public void incrementActiveIncidents() {
        activeIncidentCount.incrementAndGet();
    }

    public void decrementActiveIncidents() {
        activeIncidentCount.decrementAndGet();
    }

    public long getActiveIncidentCount() {
        return activeIncidentCount.get();
    }
}
