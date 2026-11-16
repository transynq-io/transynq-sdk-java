package io.transynq.sdk.errors;

/** No answer: the connection failed (DNS, TLS, reset). Retried when the request is safe to repeat. */
public class TransynqConnectionException extends TransynqException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param message the message
     * @param cause the cause
     */
    public TransynqConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
