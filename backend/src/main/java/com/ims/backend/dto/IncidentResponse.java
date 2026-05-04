package com.ims.backend.dto;

import com.ims.backend.model.Incident;
import com.ims.backend.model.IncidentStatus;
import com.ims.backend.model.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * API response for incident data (shields internal model from API consumers).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentResponse {

    private String id;
    private String componentId;
    private IncidentStatus status;
    private Severity severity;
    private Instant startTime;
    private Instant endTime;
    private Long mttrMinutes;
    private Instant createdAt;
    private Instant updatedAt;

    public static IncidentResponse from(Incident incident) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .componentId(incident.getComponentId())
                .status(IncidentStatus.valueOf(incident.getStatus()))
                .severity(Severity.valueOf(incident.getSeverity()))
                .startTime(incident.getStartTime())
                .endTime(incident.getEndTime())
                .mttrMinutes(incident.getMttrMinutes())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }
}
