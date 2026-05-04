package com.ims.backend.repository.document;

import com.ims.backend.model.Signal;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive MongoDB repository for raw signals.
 */
@Repository
public interface SignalRepository extends ReactiveMongoRepository<Signal, String> {

    /** All signals linked to a specific incident */
    Flux<Signal> findByIncidentId(String incidentId);

    /** All signals from a component within an incident context */
    Flux<Signal> findByComponentId(String componentId);

    /** Count signals by component – useful for metrics */
    Mono<Long> countByComponentId(String componentId);
}
