package com.ims.backend.exception;

/**
 * Thrown when the request rate exceeds the configured limit.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Rate limit exceeded. Please slow down your request rate.");
    }
}
