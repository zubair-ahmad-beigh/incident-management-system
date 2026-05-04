package com.ims.backend.repository.relational;

import com.ims.backend.model.Incident;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive R2DBC repository for Incident.
 * All operations are non-blocking – returns Mono/Flux.
 * ID type is String (CHAR(36) UUID in MySQL).
 */
@Repository
public interface IncidentRepository extends R2dbcRepository<Incident, String> {

    /**
     * Find any active (non-CLOSED) incident for a component.
     * Used by the consumer to implement debouncing / deduplication.
     */
    @Query("SELECT * FROM incidents WHERE component_id = :componentId AND status != 'CLOSED' LIMIT 1")
    Mono<Incident> findActiveByComponentId(String componentId);

    /** Dashboard: all incidents with a given status, newest first */
    Flux<Incident> findByStatusOrderByStartTimeDesc(String status);

    /** All incidents for a component */
    Flux<Incident> findByComponentIdOrderByStartTimeDesc(String componentId);
}
