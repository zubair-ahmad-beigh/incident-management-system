package com.ims.backend.repository.relational;

import com.ims.backend.model.RcaRecord;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Reactive R2DBC repository for RcaRecord.
 * ID type is String (CHAR(36) UUID in MySQL).
 */
@Repository
public interface RcaRepository extends R2dbcRepository<RcaRecord, String> {

    /** Fetch RCA for an incident (1-to-1). */
    Mono<RcaRecord> findByIncidentId(String incidentId);

    /** Check existence without loading the full entity. */
    Mono<Boolean> existsByIncidentId(String incidentId);
}
