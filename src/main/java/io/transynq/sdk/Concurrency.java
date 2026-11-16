package io.transynq.sdk;

import io.transynq.sdk.errors.StaleVersionException;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Optimistic concurrency helpers (ETag / If-Match). */
public final class Concurrency {
    private Concurrency() {
    }

    /**
     * Loads a record (with its ETag), lets {@code save} write it with {@code If-Match}, and on {@link StaleVersionException} loads it
     * again and repeats, up to {@code maxAttempts} times.
     *
     * @param load reads the record
     * @param save writes the change; receives the record and options carrying its ETag
     * @param maxAttempts attempts before the stale-version error is thrown
     * @param <T> the loaded type
     * @param <R> the saved type
     * @return the save's answer
     */
    public static <T, R> ApiResponse<R> readModifyWrite(Supplier<ApiResponse<T>> load, BiFunction<T, RequestOptions, ApiResponse<R>> save, int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        for (int attempt = 1;; attempt++) {
            ApiResponse<T> current = load.get();
            if (current.etag() == null) {
                throw new IllegalStateException("The loaded record has no ETag.");
            }
            try {
                return save.apply(current.data(), RequestOptions.ifMatch(current.etag()));
            } catch (StaleVersionException e) {
                if (attempt >= maxAttempts) {
                    throw e;
                }
            }
        }
    }
}
