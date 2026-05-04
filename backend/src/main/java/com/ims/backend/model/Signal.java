package com.ims.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document representing a raw ingested signal.
 * Collection: signals
 * Linked to an Incident via incidentId (set asynchronously by the consumer).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "signals")
public class Signal {

    @Id
    private String id;

    @Indexed
    private String componentId;

    /** Set once the consumer links this signal to an incident */
    @Indexed
    private String incidentId;

    private String type;
    private String message;
    private String rawPayload;

    @Indexed
    private Instant timestamp;

    private Instant processedAt;
}
