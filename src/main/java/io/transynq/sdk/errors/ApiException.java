package io.transynq.sdk.errors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The API answered with an error (ProblemDetails, docs/api-conventions.md section 3). */
public class ApiException extends TransynqException {
    private static final long serialVersionUID = 1L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * What an error answer said.
     *
     * @param status the HTTP status
     * @param code the stable problem code, if sent
     * @param title the title, if sent
     * @param detail the detail, if sent
     * @param type the type URI, if sent
     * @param correlationId the correlation id (body, else header)
     * @param rawBody the raw body
     * @param headers the response headers (lower-case names)
     * @param operationId the operation that failed
     */
    public record Details(int status, String code, String title, String detail, String type, String correlationId, String rawBody,
            Map<String, String> headers, String operationId) {
    }

    private final transient Details details;

    /**
     * Creates the error.
     *
     * @param message the message
     * @param details the answer's details
     */
    public ApiException(String message, Details details) {
        super(message);
        this.details = details;
    }

    /** @return the HTTP status */
    public int status() {
        return details.status();
    }

    /** @return the stable problem code ({@code validation}, {@code not-found}, a rule code...), or null */
    public String code() {
        return details.code();
    }

    /** @return the problem title, or null */
    public String title() {
        return details.title();
    }

    /** @return the problem detail, or null */
    public String detail() {
        return details.detail();
    }

    /** @return the problem type URI, or null */
    public String type() {
        return details.type();
    }

    /** @return the correlation id (body, else the {@code X-Correlation-Id} header), or null */
    public String correlationId() {
        return details.correlationId();
    }

    /** @return the raw response body */
    public String rawBody() {
        return details.rawBody();
    }

    /** @return the response headers (lower-case names) */
    public Map<String, String> headers() {
        return details.headers();
    }

    /** @return the operation that failed */
    public String operationId() {
        return details.operationId();
    }

    private static String text(JsonNode node, String name) {
        JsonNode v = node == null ? null : node.get(name);
        if (v == null || v.isNull() || v.isMissingNode()) {
            return null;
        }
        return v.isTextual() ? v.asText() : v.toString();
    }

    /**
     * Builds the typed error for an error answer.
     *
     * @param status the status
     * @param body the body
     * @param headers the headers (lower-case names)
     * @param operationId the operation
     * @param retryAfter the {@code Retry-After} delay, or null
     * @return the error
     */
    public static ApiException fromResponse(int status, byte[] body, Map<String, String> headers, String operationId, Duration retryAfter) {
        String raw = body == null ? "" : new String(body, StandardCharsets.UTF_8);
        JsonNode problem = null;
        if (!raw.isEmpty()) {
            try {
                JsonNode parsed = MAPPER.readTree(raw);
                if (parsed != null && parsed.isObject()) {
                    problem = parsed;
                }
            } catch (java.io.IOException ignored) {
                // not ProblemDetails (a proxy page): keep the raw body only
            }
        }
        String code = text(problem, "code");
        String title = text(problem, "title");
        String detail = text(problem, "detail");
        String correlationId = text(problem, "correlationId");
        if (correlationId == null) {
            correlationId = headers.get("x-correlation-id");
        }
        StringBuilder message = new StringBuilder(operationId).append(" failed with ").append(status);
        if (code != null) {
            message.append(" (").append(code).append(')');
        }
        message.append(": ").append(title != null ? title : "no title");
        if (detail != null) {
            message.append(" - ").append(detail);
        }
        if (correlationId != null) {
            message.append(" [correlation id ").append(correlationId).append(']');
        }
        Details d = new Details(status, code, title, detail, text(problem, "type"), correlationId, raw, Collections.unmodifiableMap(headers), operationId);
        String m = message.toString();
        switch (status) {
            case 400:
                Map<String, List<String>> errors = new LinkedHashMap<>();
                JsonNode e = problem == null ? null : problem.get("errors");
                if (e != null && e.isObject()) {
                    e.fields().forEachRemaining(f -> {
                        List<String> list = new ArrayList<>();
                        if (f.getValue().isArray()) {
                            f.getValue().forEach(x -> list.add(x.isTextual() ? x.asText() : x.toString()));
                        } else {
                            list.add(f.getValue().asText());
                        }
                        errors.put(f.getKey(), Collections.unmodifiableList(list));
                    });
                }
                return new ValidationException(m, d, Collections.unmodifiableMap(errors));
            case 401:
                return new AuthenticationException(m, d);
            case 403:
                return new PermissionDeniedException(m, d);
            case 404:
                return new NotFoundException(m, d);
            case 409:
                return new ConflictException(m, d);
            case 412:
                return new StaleVersionException(m, d);
            case 422:
                return new BusinessRuleException(m, d);
            case 429:
                return new RateLimitException(m, d, retryAfter);
            default:
                return status >= 500 ? new ServerException(m, d) : new ApiException(m, d);
        }
    }
}
