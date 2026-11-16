package com.transynq.sdk.errors;

import com.transynq.sdk.models.LongOperationResponse;

/** A long operation ended in {@code Failed}. */
public class LongOperationFailedException extends TransynqException {
    private static final long serialVersionUID = 1L;
    private final transient LongOperationResponse operation;

    /**
     * Creates the error.
     *
     * @param message the message
     * @param operation the final state
     */
    public LongOperationFailedException(String message, LongOperationResponse operation) {
        super(message);
        this.operation = operation;
    }

    /** @return the final state of the operation */
    public LongOperationResponse operation() {
        return operation;
    }
}
