package com.transynq.sdk.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.transynq.sdk.FileUpload;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** A request built by a generated service method. Not part of the public API. */
public final class ApiRequest {
    private static final Set<String> SAFE = Set.of("GET", "HEAD", "OPTIONS", "PUT", "DELETE");

    private final String method;
    private final String pathTemplate;
    private final String operationId;
    private final Map<String, String> pathValues = new LinkedHashMap<>();
    private final List<Map.Entry<String, String>> queryValues = new ArrayList<>();
    private final List<Map.Entry<String, String>> formTexts = new ArrayList<>();
    private final List<Map.Entry<String, FileUpload>> formFiles = new ArrayList<>();
    private byte[] json;
    private boolean hasClientId;

    /**
     * Creates a request.
     *
     * @param method the HTTP method
     * @param pathTemplate the path with {@code {name}} placeholders
     * @param operationId the OpenAPI operation id
     */
    public ApiRequest(String method, String pathTemplate, String operationId) {
        this.method = method;
        this.pathTemplate = pathTemplate;
        this.operationId = operationId;
    }

    /**
     * Formats a path, query or form value the way the API binds it.
     *
     * @param value the value
     * @return the text
     */
    public static String format(Object value) {
        if (value instanceof OffsetDateTime odt) {
            return Json.formatInstant(odt);
        }
        if (value instanceof BigDecimal d) {
            return d.toPlainString();
        }
        if (value instanceof LocalDate || value instanceof UUID) {
            return value.toString();
        }
        return String.valueOf(value);
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Sets a path parameter.
     *
     * @param name the name
     * @param value the value (required)
     */
    public void path(String name, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(operationId + ": path parameter " + name + " is required");
        }
        pathValues.put(name, encode(format(value)));
    }

    /**
     * Adds a query parameter (skipped when null; a collection repeats the key).
     *
     * @param name the name
     * @param value the value
     */
    public void query(String name, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Iterable<?> items) {
            for (Object item : items) {
                query(name, item);
            }
            return;
        }
        queryValues.add(Map.entry(name, format(value)));
    }

    /**
     * Sets the JSON body.
     *
     * @param body the body (nothing is sent when null)
     * @param clientIdCreate whether the operation is an idempotent create by client id
     */
    public void jsonBody(Object body, boolean clientIdCreate) {
        if (body == null) {
            return;
        }
        try {
            json = Json.mapper().writeValueAsBytes(body);
            if (clientIdCreate) {
                JsonNode id = Json.mapper().readTree(json).get("id");
                hasClientId = id != null && id.isTextual() && !id.asText().matches("[0-]*");
            }
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException(operationId + ": the body cannot be serialised", e);
        }
    }

    /**
     * Adds a text part.
     *
     * @param name the part name
     * @param value the value (skipped when null)
     */
    public void formText(String name, Object value) {
        if (value != null) {
            formTexts.add(Map.entry(name, format(value)));
        }
    }

    /**
     * Adds a file part.
     *
     * @param name the part name
     * @param file the file (skipped when null)
     */
    public void formFile(String name, FileUpload file) {
        if (file != null) {
            formFiles.add(Map.entry(name, file));
        }
    }

    /**
     * Adds several file parts with the same name.
     *
     * @param name the part name
     * @param files the files (skipped when null)
     */
    public void formFiles(String name, List<FileUpload> files) {
        if (files != null) {
            files.forEach(f -> formFile(name, f));
        }
    }

    /** @return the HTTP method */
    public String method() {
        return method;
    }

    /** @return the operation id */
    public String operationId() {
        return operationId;
    }

    /** @return the JSON body, or null */
    public byte[] json() {
        return json;
    }

    /** @return the text parts */
    public List<Map.Entry<String, String>> formTexts() {
        return formTexts;
    }

    /** @return the file parts */
    public List<Map.Entry<String, FileUpload>> formFiles() {
        return formFiles;
    }

    /** @return whether the body is multipart */
    public boolean isMultipart() {
        return !formTexts.isEmpty() || !formFiles.isEmpty();
    }

    /**
     * Whether the request may be sent again after a failure.
     *
     * @param idempotencyKey the call's idempotency key, or null
     * @return true when safe to repeat
     */
    public boolean retrySafe(String idempotencyKey) {
        return SAFE.contains(method) || (idempotencyKey != null && !idempotencyKey.isEmpty()) || hasClientId;
    }

    /**
     * The full URL.
     *
     * @param baseUrl the API host
     * @return the URL
     */
    public String url(String baseUrl) {
        String path = pathTemplate;
        for (Map.Entry<String, String> e : pathValues.entrySet()) {
            path = path.replace("{" + e.getKey() + "}", e.getValue());
        }
        if (path.contains("{")) {
            throw new IllegalArgumentException(operationId + ": path parameter missing in " + path);
        }
        StringBuilder sb = new StringBuilder(baseUrl.replaceAll("/+$", "")).append(path);
        for (int i = 0; i < queryValues.size(); i++) {
            sb.append(i == 0 ? '?' : '&').append(encode(queryValues.get(i).getKey())).append('=').append(encode(queryValues.get(i).getValue()));
        }
        return sb.toString();
    }

    static String toJsonText(Object value) throws JsonProcessingException {
        return Json.mapper().writeValueAsString(value);
    }
}
