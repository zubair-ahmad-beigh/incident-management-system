package com.ims.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global reactive exception handler – returns RFC 7807 ProblemDetail responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return Mono.just(buildProblem(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public Mono<ProblemDetail> handleInvalidTransition(InvalidStateTransitionException ex) {
        log.warn("Invalid state transition: {}", ex.getMessage());
        return Mono.just(buildProblem(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(RcaValidationException.class)
    public Mono<ProblemDetail> handleRcaValidation(RcaValidationException ex) {
        log.warn("RCA validation failure: {}", ex.getMessage());
        return Mono.just(buildProblem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public Mono<ProblemDetail> handleRateLimit(RateLimitExceededException ex) {
        return Mono.just(buildProblem(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ProblemDetail> handleValidation(WebExchangeBindException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", details);
        return Mono.just(buildProblem(HttpStatus.BAD_REQUEST, details));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ProblemDetail> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return Mono.just(buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"));
    }

    private ProblemDetail buildProblem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("https://ims.internal/errors/" + status.value()));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
