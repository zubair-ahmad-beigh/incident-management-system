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
 * MySQL entity representing a de-duplicated incident.
 * Uses String for id (stored as CHAR(36) UUID in MySQL).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("incidents")
public class Incident {

    @Id
    private String id;

    @Column("component_id")
    private String componentId;

    @Builder.Default
    private String status = IncidentStatus.OPEN.name();

    @Builder.Default
    private String severity = Severity.P2.name();

    @Column("start_time")
    private Instant startTime;

    @Column("end_time")
    private Instant endTime;

    /** Mean Time To Resolve in minutes – computed when incident is RESOLVED */
    @Column("mttr_minutes")
    private Long mttrMinutes;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
