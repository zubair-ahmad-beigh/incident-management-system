package com.ims.backend.util;

import com.ims.backend.model.Incident;
import com.ims.backend.model.IncidentStatus;
import com.ims.backend.model.Severity;
import com.ims.backend.repository.relational.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Scheduled utility that automatically escalates stale incidents.
 *
 * Rules (configurable; defaults shown):
 *  - OPEN incidents older than 30 min with P0 severity → auto-escalate to INVESTIGATING
 *  - This prevents P0 incidents sitting unacknowledged in production.
 *
 * This runs every minute and is intended as a safety net, not a replacement
 * for human on-call workflow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentEscalationScheduler {

    private final IncidentRepository incidentRepository;

    private static final long ESCALATION_THRESHOLD_MINUTES = 30;

    @Scheduled(fixedDelay = 60_000) // every minute
    public void escalateStaleP0Incidents() {
        Instant threshold = Instant.now().minusSeconds(ESCALATION_THRESHOLD_MINUTES * 60);

        incidentRepository.findByStatusOrderByStartTimeDesc(IncidentStatus.OPEN.name())
                .filter(incident ->
                        Severity.P0.name().equals(incident.getSeverity()) &&
                        incident.getStartTime().isBefore(threshold))
                .flatMap(incident -> {
                    log.warn("[ESCALATION] Auto-escalating stale P0 incident {} (component={})",
                            incident.getId(), incident.getComponentId());
                    incident.setStatus(IncidentStatus.INVESTIGATING.name());
                    incident.setUpdatedAt(Instant.now());
                    return incidentRepository.save(incident);
                })
                .subscribe(
                        saved -> log.info("[ESCALATION] Incident {} moved to INVESTIGATING", saved.getId()),
                        err   -> log.error("[ESCALATION] Error during escalation", err)
                );
    }
}
