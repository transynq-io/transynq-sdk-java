package com.transynq.sdk.errors;

/** Base of every error the SDK raises (unchecked). */
public class TransynqException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param message the message
     */
    public TransynqException(String message) {
        super(message);
    }

    /**
     * Creates the error.
     *
     * @param message the message
     * @param cause the cause
     */
    public TransynqException(String message, Throwable cause) {
        super(message, cause);
    }
}
