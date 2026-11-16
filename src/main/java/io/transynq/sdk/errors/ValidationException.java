package io.transynq.sdk.errors;

import java.util.List;
import java.util.Map;

/** 400: malformed input; {@link #errors()} lists the messages per field. */
public class ValidationException extends ApiException {
    private static final long serialVersionUID = 1L;
    private final transient Map<String, List<String>> errors;

    /**
     * Creates the error.
     *
     * @param message the message
     * @param details the answer's details
     * @param errors messages per field
     */
    public ValidationException(String message, ApiException.Details details, Map<String, List<String>> errors) {
        super(message, details);
        this.errors = errors;
    }

    /** @return messages per field */
    public Map<String, List<String>> errors() {
        return errors;
    }
}
