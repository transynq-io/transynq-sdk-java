package com.transynq.sdk.errors;

/** 412: {@code If-Match} does not match the current ETag; reload and try again. */
public class StaleVersionException extends ApiException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param message the message
     * @param details the answer's details
     */
    public StaleVersionException(String message, ApiException.Details details) {
        super(message, details);
    }
}
