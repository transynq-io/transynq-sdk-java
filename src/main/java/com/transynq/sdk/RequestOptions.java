package com.transynq.sdk;

import java.time.Duration;
import java.util.Map;

/**
 * Per-call options.
 *
 * @param ifMatch sent as {@code If-Match} (PUT and DELETE require it; take it from {@link ApiResponse#etag()})
 * @param idempotencyKey sent as {@code Idempotency-Key}; also makes a POST retryable
 * @param headers extra request headers
 * @param timeout overrides the client's timeout for this call
 * @param maxRetries overrides the client's retries for this call
 * @param correlationId overrides the generated {@code X-Correlation-Id}
 */
public record RequestOptions(String ifMatch, String idempotencyKey, Map<String, String> headers, Duration timeout, Integer maxRetries,
        String correlationId) {
    /** Copies the headers. */
    public RequestOptions {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    /**
     * Options carrying {@code If-Match}.
     *
     * @param etag the ETag
     * @return the options
     */
    public static RequestOptions ifMatch(String etag) {
        return new RequestOptions(etag, null, null, null, null, null);
    }

    /**
     * Options carrying an {@code Idempotency-Key}.
     *
     * @param key the key
     * @return the options
     */
    public static RequestOptions idempotencyKey(String key) {
        return new RequestOptions(null, key, null, null, null, null);
    }

    /** @return options carrying a new random {@code Idempotency-Key} */
    public static RequestOptions newIdempotencyKey() {
        return idempotencyKey(Ids.newIdempotencyKey());
    }

    /**
     * A copy with another retry count.
     *
     * @param retries the retries
     * @return the copy
     */
    public RequestOptions withMaxRetries(int retries) {
        return new RequestOptions(ifMatch, idempotencyKey, headers, timeout, retries, correlationId);
    }

    /**
     * A copy with another timeout.
     *
     * @param value the timeout
     * @return the copy
     */
    public RequestOptions withTimeout(Duration value) {
        return new RequestOptions(ifMatch, idempotencyKey, headers, value, maxRetries, correlationId);
    }
}
