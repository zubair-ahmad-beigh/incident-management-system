package com.ims.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * MySQL entity for Root Cause Analysis records.
 * One-to-one with Incident. Uses String ids (CHAR(36) UUID in MySQL).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("rca")
public class RcaRecord {

    @Id
    private String id;

    @Column("incident_id")
    private String incidentId;

    @Column("root_cause")
    private String rootCause;

    @Column("fix_applied")
    private String fixApplied;

    @Column("prevention_steps")
    private String preventionSteps;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
