package com.ims.backend.exception;

/**
 * Thrown when RCA validation fails (missing or incomplete fields).
 */
public class RcaValidationException extends RuntimeException {

    public RcaValidationException(String message) {
        super(message);
    }
}
