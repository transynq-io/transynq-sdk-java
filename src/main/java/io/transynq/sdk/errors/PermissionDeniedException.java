package io.transynq.sdk.errors;

/** 403: signed in but not allowed (or a missing CSRF token). */
public class PermissionDeniedException extends ApiException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param message the message
     * @param details the answer's details
     */
    public PermissionDeniedException(String message, ApiException.Details details) {
        super(message, details);
    }
}
