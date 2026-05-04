package com.ims.backend;

import com.ims.backend.model.Severity;
import com.ims.backend.strategy.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for pluggable AlertStrategy pattern.
 */
class AlertStrategyTest {

    private final AlertStrategyResolver resolver = new AlertStrategyResolver(
            List.of(new RdbmsFailureStrategy(), new CacheFailureStrategy(), new NetworkFailureStrategy())
    );

    @Test
    @DisplayName("DB_FAILURE signal type resolves to P0")
    void dbFailure_isP0() {
        assertThat(resolver.resolve("DB_FAILURE", "order-service")).isEqualTo(Severity.P0);
    }

    @Test
    @DisplayName("CACHE_MISS signal type resolves to P2")
    void cacheMiss_isP2() {
        assertThat(resolver.resolve("CACHE_MISS", "product-service")).isEqualTo(Severity.P2);
    }

    @Test
    @DisplayName("NETWORK_ERROR resolves to P1")
    void networkError_isP1() {
        assertThat(resolver.resolve("NETWORK_ERROR", "payment-service")).isEqualTo(Severity.P1);
    }

    @Test
    @DisplayName("Unknown signal type defaults to P2")
    void unknownType_defaultsToP2() {
        assertThat(resolver.resolve("UNKNOWN_TYPE", "some-service")).isEqualTo(Severity.P2);
    }

    @Test
    @DisplayName("Case-insensitive strategy matching")
    void caseInsensitive() {
        assertThat(resolver.resolve("db_failure", "order-service")).isEqualTo(Severity.P0);
    }
}
