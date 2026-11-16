package com.transynq.sdk;

import com.transynq.sdk.credentials.Credential;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/** Settings of a {@link TransynqClient}. Build with {@link #builder(String)}. */
public final class TransynqClientOptions {
    /**
     * What a hook sees of a request about to be sent (credentials redacted).
     *
     * @param method the HTTP method
     * @param url the URL
     * @param operationId the operation id
     * @param attempt 1 for the first attempt
     * @param headers the request headers, redacted
     */
    public record RequestInfo(String method, String url, String operationId, int attempt, Map<String, String> headers) {
    }

    /**
     * What a hook sees of an answer (cookies redacted).
     *
     * @param request the request
     * @param statusCode the status
     * @param elapsed time to the response headers
     * @param headers the response headers, redacted
     */
    public record ResponseInfo(RequestInfo request, int statusCode, Duration elapsed, Map<String, String> headers) {
    }

    /** Waits between retries and polls (replaceable in tests). */
    @FunctionalInterface
    public interface Sleeper {
        /**
         * Waits.
         *
         * @param delay the delay
         * @throws InterruptedException when interrupted
         */
        void sleep(Duration delay) throws InterruptedException;
    }

    private final String baseUrl;
    private final Credential credential;
    private final Duration timeout;
    private final int maxRetries;
    private final Duration retryBaseDelay;
    private final Duration maxRetryDelay;
    private final Duration maxRetryAfter;
    private final String userAgentSuffix;
    private final HttpClient httpClient;
    private final Executor executor;
    private final System.Logger logger;
    private final Consumer<RequestInfo> onRequest;
    private final Consumer<ResponseInfo> onResponse;
    private final Sleeper sleeper;
    private final DoubleSupplier jitter;

    private TransynqClientOptions(Builder b) {
        this.baseUrl = Objects.requireNonNull(b.baseUrl, "baseUrl");
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new IllegalArgumentException("baseUrl must be an absolute http(s) URL");
        }
        if (b.maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be 0 or more");
        }
        this.credential = b.credential;
        this.timeout = b.timeout;
        this.maxRetries = b.maxRetries;
        this.retryBaseDelay = b.retryBaseDelay;
        this.maxRetryDelay = b.maxRetryDelay;
        this.maxRetryAfter = b.maxRetryAfter;
        this.userAgentSuffix = b.userAgentSuffix;
        this.httpClient = b.httpClient != null ? b.httpClient
                : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).followRedirects(HttpClient.Redirect.NEVER).build();
        this.executor = b.executor != null ? b.executor : ForkJoinPool.commonPool();
        this.logger = b.logger != null ? b.logger : System.getLogger("com.transynq.sdk");
        this.onRequest = b.onRequest;
        this.onResponse = b.onResponse;
        this.sleeper = b.sleeper != null ? b.sleeper : d -> Thread.sleep(d.toMillis());
        this.jitter = b.jitter != null ? b.jitter : Math::random;
    }

    /**
     * A builder.
     *
     * @param baseUrl the API host, for example {@code https://api.example.com}
     * @return the builder
     */
    public static Builder builder(String baseUrl) {
        return new Builder(baseUrl);
    }

    /** @return the API host */
    public String baseUrl() {
        return baseUrl;
    }

    /** @return the credential, or null */
    public Credential credential() {
        return credential;
    }

    /** @return the per-attempt timeout */
    public Duration timeout() {
        return timeout;
    }

    /** @return the retries after the first attempt */
    public int maxRetries() {
        return maxRetries;
    }

    /** @return the first back-off delay */
    public Duration retryBaseDelay() {
        return retryBaseDelay;
    }

    /** @return the cap of a computed back-off delay */
    public Duration maxRetryDelay() {
        return maxRetryDelay;
    }

    /** @return the longest {@code Retry-After} waited for */
    public Duration maxRetryAfter() {
        return maxRetryAfter;
    }

    /** @return the user agent suffix, or null */
    public String userAgentSuffix() {
        return userAgentSuffix;
    }

    /** @return the HTTP client */
    public HttpClient httpClient() {
        return httpClient;
    }

    /** @return the executor of the {@code *Async} methods */
    public Executor executor() {
        return executor;
    }

    /** @return the debug logger */
    public System.Logger logger() {
        return logger;
    }

    /** @return the request hook, or null */
    public Consumer<RequestInfo> onRequest() {
        return onRequest;
    }

    /** @return the response hook, or null */
    public Consumer<ResponseInfo> onResponse() {
        return onResponse;
    }

    /** @return the sleeper */
    public Sleeper sleeper() {
        return sleeper;
    }

    /** @return the jitter source in [0, 1) */
    public DoubleSupplier jitter() {
        return jitter;
    }

    /** Builds {@link TransynqClientOptions}. */
    public static final class Builder {
        private final String baseUrl;
        private Credential credential;
        private Duration timeout = Duration.ofSeconds(60);
        private int maxRetries = 2;
        private Duration retryBaseDelay = Duration.ofMillis(500);
        private Duration maxRetryDelay = Duration.ofSeconds(8);
        private Duration maxRetryAfter = Duration.ofSeconds(60);
        private String userAgentSuffix;
        private HttpClient httpClient;
        private Executor executor;
        private System.Logger logger;
        private Consumer<RequestInfo> onRequest;
        private Consumer<ResponseInfo> onResponse;
        private Sleeper sleeper;
        private DoubleSupplier jitter;

        private Builder(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        /**
         * Sets how requests authenticate.
         *
         * @param value the credential
         * @return this builder
         */
        public Builder credential(Credential value) {
            this.credential = value;
            return this;
        }

        /**
         * Sets the time allowed for one attempt (default 60 s).
         *
         * @param value the timeout
         * @return this builder
         */
        public Builder timeout(Duration value) {
            this.timeout = Objects.requireNonNull(value);
            return this;
        }

        /**
         * Sets the retries after the first attempt (default 2; 0 turns retries off).
         *
         * @param value the retries
         * @return this builder
         */
        public Builder maxRetries(int value) {
            this.maxRetries = value;
            return this;
        }

        /**
         * Sets the first back-off delay (default 500 ms).
         *
         * @param value the delay
         * @return this builder
         */
        public Builder retryBaseDelay(Duration value) {
            this.retryBaseDelay = Objects.requireNonNull(value);
            return this;
        }

        /**
         * Sets the cap of a computed back-off delay (default 8 s).
         *
         * @param value the delay
         * @return this builder
         */
        public Builder maxRetryDelay(Duration value) {
            this.maxRetryDelay = Objects.requireNonNull(value);
            return this;
        }

        /**
         * Sets the longest {@code Retry-After} waited for (default 60 s).
         *
         * @param value the delay
         * @return this builder
         */
        public Builder maxRetryAfter(Duration value) {
            this.maxRetryAfter = Objects.requireNonNull(value);
            return this;
        }

        /**
         * Sets a suffix of the {@code User-Agent}, such as {@code my-integration/2.1}.
         *
         * @param value the suffix
         * @return this builder
         */
        public Builder userAgentSuffix(String value) {
            this.userAgentSuffix = value;
            return this;
        }

        /**
         * Sets the HTTP client (proxy, TLS, executor...). Do not give it a cookie handler: the SDK keeps the session cookie.
         *
         * @param value the client
         * @return this builder
         */
        public Builder httpClient(HttpClient value) {
            this.httpClient = value;
            return this;
        }

        /**
         * Sets the executor of the {@code *Async} methods (default the common pool).
         *
         * @param value the executor
         * @return this builder
         */
        public Builder executor(Executor value) {
            this.executor = value;
            return this;
        }

        /**
         * Sets the debug logger (default {@code System.getLogger("com.transynq.sdk")}).
         *
         * @param value the logger
         * @return this builder
         */
        public Builder logger(System.Logger value) {
            this.logger = value;
            return this;
        }

        /**
         * Sets a hook called before each attempt.
         *
         * @param value the hook
         * @return this builder
         */
        public Builder onRequest(Consumer<RequestInfo> value) {
            this.onRequest = value;
            return this;
        }

        /**
         * Sets a hook called after each answer.
         *
         * @param value the hook
         * @return this builder
         */
        public Builder onResponse(Consumer<ResponseInfo> value) {
            this.onResponse = value;
            return this;
        }

        /**
         * Replaces the wait between retries and polls (tests).
         *
         * @param value the sleeper
         * @return this builder
         */
        public Builder sleeper(Sleeper value) {
            this.sleeper = value;
            return this;
        }

        /**
         * Replaces the jitter source (tests).
         *
         * @param value values in [0, 1)
         * @return this builder
         */
        public Builder jitter(DoubleSupplier value) {
            this.jitter = value;
            return this;
        }

        /** @return the options */
        public TransynqClientOptions build() {
            return new TransynqClientOptions(this);
        }
    }
}
