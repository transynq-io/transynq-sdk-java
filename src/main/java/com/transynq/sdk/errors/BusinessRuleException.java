package com.transynq.sdk.errors;

/** 422: a business rule refused the request; {@link #code()} is the rule. */
public class BusinessRuleException extends ApiException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param message the message
     * @param details the answer's details
     */
    public BusinessRuleException(String message, ApiException.Details details) {
        super(message, details);
    }
}
