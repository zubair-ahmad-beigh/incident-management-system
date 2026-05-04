package com.ims.backend.exception;

/**
 * Thrown when a requested resource (incident, RCA) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(String.format("%s with id '%s' not found", resource, id));
    }
}
