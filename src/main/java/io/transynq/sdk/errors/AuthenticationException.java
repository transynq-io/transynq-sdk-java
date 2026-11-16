package io.transynq.sdk.errors;

/** 401: not signed in, session ended, invalid key or token. */
public class AuthenticationException extends ApiException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param message the message
     * @param details the answer's details
     */
    public AuthenticationException(String message, ApiException.Details details) {
        super(message, details);
    }
}
