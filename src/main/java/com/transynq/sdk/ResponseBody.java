package com.transynq.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.transynq.sdk.internal.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * A buffered body of an operation whose contract does not declare its response (JSON, a small file, or nothing).
 *
 * @param content the raw bytes
 * @param contentType the media type, or null
 * @param fileName the file name from {@code Content-Disposition}, or null
 */
public record ResponseBody(byte[] content, String contentType, String fileName) {
    /** @return true when the body is empty */
    public boolean isEmpty() {
        return content.length == 0;
    }

    /** @return the body as UTF-8 text */
    public String text() {
        return new String(content, StandardCharsets.UTF_8);
    }

    /** @return the body parsed as JSON (decimals as {@code BigDecimal}), or null when empty */
    public JsonNode json() {
        if (isEmpty()) {
            return null;
        }
        try {
            return Json.mapper().readTree(content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The body deserialised as a model.
     *
     * @param type the model class
     * @param <T> the model type
     * @return the value, or null when empty
     */
    public <T> T as(Class<T> type) {
        if (isEmpty()) {
            return null;
        }
        try {
            return Json.mapper().readValue(content, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
