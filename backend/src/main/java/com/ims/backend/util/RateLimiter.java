package com.ims.backend.util;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import com.ims.backend.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token-bucket rate limiter keyed by remote IP address.
 * Uses Bucket4j for lock-free, thread-safe rate limiting.
 *
 * Configuration (application.yml):
 *   ims.rate-limit.signals-per-second  (refill rate)
 *   ims.rate-limit.burst-capacity      (max tokens in bucket)
 */
@Slf4j
@Component
public class RateLimiter {

    private final long signalsPerSecond;
    private final long burstCapacity;

    /** Per-IP bucket store. Eviction would be added via Caffeine in production. */
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimiter(
            @Value("${ims.rate-limit.signals-per-second:10000}") long signalsPerSecond,
            @Value("${ims.rate-limit.burst-capacity:20000}") long burstCapacity) {
        this.signalsPerSecond = signalsPerSecond;
        this.burstCapacity    = burstCapacity;
    }

    /**
     * Consumes one token from the bucket for the given key.
     *
     * @throws RateLimitExceededException if the bucket is empty
     */
    public void consume(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, this::newBucket);
        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for key: {}", key);
            throw new RateLimitExceededException();
        }
    }

    private Bucket newBucket(String key) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(burstCapacity)
                .refillGreedy(signalsPerSecond, Duration.ofSeconds(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
