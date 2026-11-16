package com.transynq.sdk.errors;

/** 404: missing, or outside the caller's scope. */
public class NotFoundException extends ApiException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param message the message
     * @param details the answer's details
     */
    public NotFoundException(String message, ApiException.Details details) {
        super(message, details);
    }
}
