package com.ims.backend.strategy;

import com.ims.backend.model.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Resolves the appropriate {@link AlertStrategy} for a given signal type.
 * Falls back to P2 (DefaultAlertStrategy) if no specific strategy matches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertStrategyResolver {

    private final List<AlertStrategy> strategies;

    /**
     * Determines severity by scanning registered strategies in order.
     * First match wins. Defaults to P2 if no strategy matches.
     */
    public Severity resolve(String signalType, String componentId) {
        return strategies.stream()
                .filter(s -> s.supports(signalType))
                .findFirst()
                .map(s -> {
                    Severity sev = s.determineSeverity(signalType, componentId);
                    log.debug("Strategy {} resolved {} → {}", s.getClass().getSimpleName(), signalType, sev);
                    return sev;
                })
                .orElseGet(() -> {
                    log.debug("No strategy matched for type '{}', defaulting to P2", signalType);
                    return Severity.P2;
                });
    }
}
