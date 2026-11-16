package com.transynq.sdk.errors;

import java.time.Duration;

/** 429: rate limited; {@link #retryAfter()} says when to try again. */
public class RateLimitException extends ApiException {
    private static final long serialVersionUID = 1L;
    private final Duration retryAfter;

    /**
     * Creates the error.
     *
     * @param message the message
     * @param details the answer's details
     * @param retryAfter the {@code Retry-After} delay, or null
     */
    public RateLimitException(String message, ApiException.Details details, Duration retryAfter) {
        super(message, details);
        this.retryAfter = retryAfter;
    }

    /** @return the {@code Retry-After} delay, or null */
    public Duration retryAfter() {
        return retryAfter;
    }
}
