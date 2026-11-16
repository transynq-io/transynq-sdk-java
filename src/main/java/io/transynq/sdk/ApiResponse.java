package io.transynq.sdk;

import java.util.Map;

/**
 * An answer of the API: the data plus the headers integrators need.
 *
 * @param statusCode the HTTP status
 * @param data the deserialised body (null when empty)
 * @param headers the response headers (lower-case names; repeated values joined by {@code ", "})
 * @param <T> the data type
 */
public record ApiResponse<T>(int statusCode, T data, Map<String, String> headers) {
    /** @return the {@code ETag} header verbatim (quotes kept), to send back as {@code If-Match}; or null */
    public String etag() {
        return headers.get("etag");
    }

    /** @return the contract version that answered ({@code X-Api-Version}), or null */
    public String apiVersion() {
        return headers.get("x-api-version");
    }

    /** @return the request's correlation id ({@code X-Correlation-Id}), or null */
    public String correlationId() {
        return headers.get("x-correlation-id");
    }

    /** @return the {@code Location} header (a 202 points at {@code /api/v1/long-operations/{id}}), or null */
    public String location() {
        return headers.get("location");
    }

    /** @return whether the API accepted long-running work (202) */
    public boolean isAccepted() {
        return statusCode == 202;
    }
}
