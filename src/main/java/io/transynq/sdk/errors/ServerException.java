package io.transynq.sdk.errors;

/** 5xx: the API failed (quote {@link #correlationId()} to support). */
public class ServerException extends ApiException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param message the message
     * @param details the answer's details
     */
    public ServerException(String message, ApiException.Details details) {
        super(message, details);
    }
}
