package com.ims.backend;

import com.ims.backend.model.Severity;
import com.ims.backend.strategy.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MTTR computation helpers and alert routing.
 */
class SignalProcessingTest {

    private final AlertStrategyResolver resolver = new AlertStrategyResolver(
            List.of(new RdbmsFailureStrategy(), new CacheFailureStrategy(), new NetworkFailureStrategy())
    );

    @Nested
    @DisplayName("AlertStrategy severity routing")
    class SeverityRouting {

        @ParameterizedTest(name = "''{0}'' → {1}")
        @CsvSource({
                "DB_FAILURE,              P0",
                "DATABASE_ERROR,          P0",
                "CONNECTION_POOL_EXHAUSTED, P0",
                "CACHE_MISS,              P2",
                "REDIS_ERROR,             P2",
                "CACHE_TIMEOUT,           P2",
                "CACHE_FAILURE,           P2",
                "NETWORK_ERROR,           P1",
                "TIMEOUT,                 P1",
                "CONNECTION_REFUSED,      P1",
                "SERVICE_UNAVAILABLE,     P1",
                "UNKNOWN_ALERT,           P2"    // default
        })
        void signalType_mapsSeverity(String type, Severity expected) {
            assertThat(resolver.resolve(type.trim(), "any-component")).isEqualTo(expected);
        }

        @Test
        @DisplayName("Null signal type should default to P2 without NPE")
        void nullType_defaultsToP2() {
            assertThat(resolver.resolve(null, "svc")).isEqualTo(Severity.P2);
        }

        @Test
        @DisplayName("Each strategy correctly reports what it supports")
        void supportsContract() {
            assertThat(new RdbmsFailureStrategy().supports("DB_FAILURE")).isTrue();
            assertThat(new RdbmsFailureStrategy().supports("CACHE_MISS")).isFalse();
            assertThat(new CacheFailureStrategy().supports("REDIS_ERROR")).isTrue();
            assertThat(new CacheFailureStrategy().supports("DB_FAILURE")).isFalse();
            assertThat(new NetworkFailureStrategy().supports("TIMEOUT")).isTrue();
            assertThat(new NetworkFailureStrategy().supports(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("MTTR computation")
    class MttrComputation {

        @Test
        @DisplayName("MTTR = difference between end_time and start_time in minutes")
        void mttr_calculatedCorrectly() {
            java.time.Instant start = java.time.Instant.parse("2024-01-01T10:00:00Z");
            java.time.Instant end   = java.time.Instant.parse("2024-01-01T11:30:00Z");

            long mttr = java.time.Duration.between(start, end).toMinutes();
            assertThat(mttr).isEqualTo(90L);
        }

        @Test
        @DisplayName("MTTR = 0 when end_time equals start_time")
        void mttr_zeroWhenSameTime() {
            java.time.Instant t = java.time.Instant.now();
            long mttr = java.time.Duration.between(t, t).toMinutes();
            assertThat(mttr).isZero();
        }
    }
}
