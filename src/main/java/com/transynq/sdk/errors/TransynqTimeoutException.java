package com.transynq.sdk.errors;

/** No answer within the timeout. */
public class TransynqTimeoutException extends TransynqConnectionException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param message the message
     * @param cause the cause
     */
    public TransynqTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
