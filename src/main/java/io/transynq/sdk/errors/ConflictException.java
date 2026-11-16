package io.transynq.sdk.errors;

/** 409: the data was changed by someone else. */
public class ConflictException extends ApiException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param message the message
     * @param details the answer's details
     */
    public ConflictException(String message, ApiException.Details details) {
        super(message, details);
    }
}
