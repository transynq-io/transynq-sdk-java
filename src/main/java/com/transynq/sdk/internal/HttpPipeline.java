package com.transynq.sdk.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.transynq.sdk.ApiResponse;
import com.transynq.sdk.FileDownload;
import com.transynq.sdk.FileUpload;
import com.transynq.sdk.RequestOptions;
import com.transynq.sdk.ResponseBody;
import com.transynq.sdk.SdkInfo;
import com.transynq.sdk.TransynqClientOptions;
import com.transynq.sdk.credentials.Credential;
import com.transynq.sdk.credentials.CredentialRequest;
import com.transynq.sdk.errors.ApiException;
import com.transynq.sdk.errors.TransynqConnectionException;
import com.transynq.sdk.errors.TransynqException;
import com.transynq.sdk.errors.TransynqTimeoutException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Sends requests: credentials, standard headers, timeouts, retries with back-off, error mapping. Not part of the public API. */
public final class HttpPipeline implements CredentialRequest.SignInTransport {
    private static final Set<String> REDACTED = Set.of("authorization", "cookie", "set-cookie", "x-transynq-apikey", "x-csrf-token", "proxy-authorization");
    private static final Set<Integer> RETRYABLE = Set.of(408, 429, 500, 502, 503, 504);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TransynqClientOptions options;
    private final HttpClient http;
    private final String userAgent;

    /**
     * Creates the pipeline.
     *
     * @param options the client options
     */
    public HttpPipeline(TransynqClientOptions options) {
        this.options = options;
        this.http = options.httpClient();
        String suffix = options.userAgentSuffix();
        this.userAgent = "transynq-sdk-java/" + SdkInfo.VERSION + (suffix == null || suffix.isBlank() ? "" : " " + suffix.trim());
    }

    /** @return the options */
    public TransynqClientOptions options() {
        return options;
    }

    /** @return the {@code User-Agent} sent */
    public String userAgent() {
        return userAgent;
    }

    /**
     * Runs a call on the client's executor.
     *
     * @param call the call
     * @param <T> the result type
     * @return the future
     */
    public <T> CompletableFuture<T> async(Supplier<T> call) {
        return CompletableFuture.supplyAsync(call, options.executor());
    }

    /**
     * Sends and deserialises JSON.
     *
     * @param req the request
     * @param type the response type
     * @param callOptions per-call options, or null
     * @param <T> the response type
     * @return the response
     */
    public <T> ApiResponse<T> sendJson(ApiRequest req, TypeReference<T> type, RequestOptions callOptions) {
        HttpResponse<InputStream> response = send(req, callOptions);
        byte[] bytes = readAll(response.body());
        try {
            T data = bytes.length == 0 ? null : Json.mapper().readValue(bytes, type);
            return new ApiResponse<>(response.statusCode(), data, headersOf(response));
        } catch (IOException e) {
            throw new TransynqException(req.operationId() + ": the response could not be read", e);
        }
    }

    /**
     * Sends and buffers the body.
     *
     * @param req the request
     * @param callOptions per-call options, or null
     * @return the response
     */
    public ApiResponse<ResponseBody> sendRaw(ApiRequest req, RequestOptions callOptions) {
        HttpResponse<InputStream> response = send(req, callOptions);
        Map<String, String> headers = headersOf(response);
        return new ApiResponse<>(response.statusCode(), new ResponseBody(readAll(response.body()), mediaType(headers), fileName(headers.get("content-disposition"))), headers);
    }

    /**
     * Sends and streams the body.
     *
     * @param req the request
     * @param callOptions per-call options, or null
     * @return the response (close its data)
     */
    public ApiResponse<FileDownload> sendDownload(ApiRequest req, RequestOptions callOptions) {
        HttpResponse<InputStream> response = send(req, callOptions);
        Map<String, String> headers = headersOf(response);
        String length = headers.get("content-length");
        FileDownload file = new FileDownload(response.body(), mediaType(headers), fileName(headers.get("content-disposition")),
                length == null ? null : Long.valueOf(length));
        return new ApiResponse<>(response.statusCode(), file, headers);
    }

    private static String mediaType(Map<String, String> headers) {
        String ct = headers.get("content-type");
        return ct == null ? null : ct.split(";")[0].trim();
    }

    /**
     * The file name of a {@code Content-Disposition} header.
     *
     * @param disposition the header
     * @return the name, or null
     */
    public static String fileName(String disposition) {
        if (disposition == null) {
            return null;
        }
        Matcher star = Pattern.compile("filename\\*\\s*=\\s*(?:UTF-8'')?([^;]+)", Pattern.CASE_INSENSITIVE).matcher(disposition);
        if (star.find()) {
            return java.net.URLDecoder.decode(star.group(1).trim().replace("\"", ""), StandardCharsets.UTF_8);
        }
        Matcher plain = Pattern.compile("filename\\s*=\\s*\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE).matcher(disposition);
        return plain.find() ? plain.group(1).trim() : null;
    }

    private static byte[] readAll(InputStream in) {
        try (in) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String newCorrelationId() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private Map<String, String> standardHeaders(String correlationId, RequestOptions o) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("User-Agent", userAgent);
        h.put("Accept", "application/json");
        h.put("X-Correlation-Id", correlationId);
        if (o != null) {
            if (o.ifMatch() != null) {
                h.put("If-Match", o.ifMatch());
            }
            if (o.idempotencyKey() != null) {
                h.put("Idempotency-Key", o.idempotencyKey());
            }
            h.putAll(o.headers());
        }
        return h;
    }

    private static Map<String, String> redact(Map<String, String> headers) {
        Map<String, String> r = new LinkedHashMap<>();
        headers.forEach((k, v) -> r.put(k, REDACTED.contains(k.toLowerCase(Locale.ROOT)) ? "[redacted]" : v));
        return r;
    }

    /**
     * Response headers as lower-case names (repeated values joined by {@code ", "}).
     *
     * @param response the response
     * @return the headers
     */
    public static Map<String, String> headersOf(HttpResponse<?> response) {
        Map<String, String> h = new TreeMap<>();
        response.headers().map().forEach((k, v) -> h.put(k.toLowerCase(Locale.ROOT), String.join(", ", v)));
        return h;
    }

    private Duration backOff(int attempt) {
        double exponential = options.retryBaseDelay().toMillis() * Math.pow(2, attempt - 1);
        double capped = Math.min(options.maxRetryDelay().toMillis(), exponential);
        return Duration.ofMillis((long) (capped * options.jitter().getAsDouble()));
    }

    /**
     * {@code Retry-After} as a duration.
     *
     * @param value the header
     * @return the delay, or null
     */
    public static Duration retryAfter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (v.chars().allMatch(Character::isDigit)) {
            return Duration.ofSeconds(Long.parseLong(v));
        }
        try {
            Duration d = Duration.between(ZonedDateTime.now(), ZonedDateTime.parse(v, DateTimeFormatter.RFC_1123_DATE_TIME));
            return d.isNegative() ? Duration.ZERO : d;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void sleep(Duration delay) {
        try {
            options.sleeper().sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransynqException("interrupted while waiting to retry", e);
        }
    }

    private HttpRequest.BodyPublisher body(ApiRequest req, Map<String, String> headers) {
        if (req.json() != null) {
            headers.put("Content-Type", "application/json; charset=utf-8");
            return HttpRequest.BodyPublishers.ofByteArray(req.json());
        }
        if (req.isMultipart()) {
            String boundary = "transynq-" + UUID.randomUUID().toString().replace("-", "");
            headers.put("Content-Type", "multipart/form-data; boundary=" + boundary);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (Map.Entry<String, String> t : req.formTexts()) {
                write(out, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + t.getKey() + "\"\r\n\r\n" + t.getValue() + "\r\n");
            }
            for (Map.Entry<String, FileUpload> f : req.formFiles()) {
                write(out, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + f.getKey() + "\"; filename=\"" + f.getValue().fileName().replace("\"", "")
                        + "\"\r\nContent-Type: " + f.getValue().contentType() + "\r\n\r\n");
                out.writeBytes(f.getValue().content());
                write(out, "\r\n");
            }
            write(out, "--" + boundary + "--\r\n");
            return HttpRequest.BodyPublishers.ofByteArray(out.toByteArray());
        }
        return HttpRequest.BodyPublishers.noBody();
    }

    private static void write(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    private HttpResponse<InputStream> exchange(String method, String url, Map<String, String> headers, HttpRequest.BodyPublisher body, Duration timeout)
            throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).method(method, body);
        headers.forEach(b::header);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofInputStream());
    }

    private void log(String operationId, int attempt, Object outcome, long elapsedMs, String correlationId) {
        System.Logger logger = options.logger();
        if (logger.isLoggable(System.Logger.Level.DEBUG)) {
            logger.log(System.Logger.Level.DEBUG, "Transynq {0} attempt {1} -> {2} in {3} ms (correlation id {4})", operationId, attempt, outcome, elapsedMs, correlationId);
        }
    }

    /**
     * The send loop; returns a successful response (body unread).
     *
     * @param req the request
     * @param o per-call options, or null
     * @return the response
     */
    public HttpResponse<InputStream> send(ApiRequest req, RequestOptions o) {
        String correlationId = o != null && o.correlationId() != null ? o.correlationId() : newCorrelationId();
        int maxRetries = Math.max(0, o != null && o.maxRetries() != null ? o.maxRetries() : options.maxRetries());
        Duration timeout = o != null && o.timeout() != null ? o.timeout() : options.timeout();
        Credential credential = options.credential();
        boolean retrySafe = req.retrySafe(o == null ? null : o.idempotencyKey());
        String url = req.url(options.baseUrl());
        boolean authRetried = false;
        for (int attempt = 1;; attempt++) {
            Map<String, String> headers = standardHeaders(correlationId, o);
            CredentialRequest credRequest = new CredentialRequest(req.method(), url, headers, this);
            if (credential != null) {
                credential.apply(credRequest);
            }
            HttpRequest.BodyPublisher body = body(req, headers);
            TransynqClientOptions.RequestInfo info = new TransynqClientOptions.RequestInfo(req.method(), url, req.operationId(), attempt, redact(headers));
            if (options.onRequest() != null) {
                options.onRequest().accept(info);
            }
            long started = System.nanoTime();
            HttpResponse<InputStream> response;
            try {
                response = exchange(req.method(), url, headers, body, timeout);
            } catch (HttpTimeoutException e) {
                log(req.operationId(), attempt, "timeout", (System.nanoTime() - started) / 1_000_000, correlationId);
                if (retrySafe && attempt <= maxRetries) {
                    sleep(backOff(attempt));
                    continue;
                }
                throw new TransynqTimeoutException(req.operationId() + " timed out after " + timeout + " [correlation id " + correlationId + "]", e);
            } catch (IOException e) {
                log(req.operationId(), attempt, e.getClass().getSimpleName(), (System.nanoTime() - started) / 1_000_000, correlationId);
                if (retrySafe && attempt <= maxRetries) {
                    sleep(backOff(attempt));
                    continue;
                }
                throw new TransynqConnectionException(req.operationId() + " could not reach the API: " + e + " [correlation id " + correlationId + "]", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TransynqException(req.operationId() + " was interrupted", e);
            }
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            int status = response.statusCode();
            log(req.operationId(), attempt, status, elapsedMs, correlationId);
            Map<String, String> responseHeaders = headersOf(response);
            if (options.onResponse() != null) {
                options.onResponse().accept(new TransynqClientOptions.ResponseInfo(info, status, Duration.ofMillis(elapsedMs), redact(responseHeaders)));
            }
            if (status >= 200 && status < 300) {
                return response;
            }
            if (status == 401 && credential != null && !authRetried && credential.refreshAfterUnauthorized(credRequest)) {
                authRetried = true;
                attempt--;
                readAll(response.body());
                continue;
            }
            Duration retryAfter = retryAfter(responseHeaders.get("retry-after"));
            if (RETRYABLE.contains(status) && (retrySafe || status == 429) && attempt <= maxRetries
                    && (retryAfter == null || retryAfter.compareTo(options.maxRetryAfter()) <= 0)) {
                readAll(response.body());
                sleep(retryAfter != null ? retryAfter : backOff(attempt));
                continue;
            }
            throw ApiException.fromResponse(status, readAll(response.body()), responseHeaders, req.operationId(), retryAfter);
        }
    }

    @Override
    public CredentialRequest.SignInResponse postJson(String path, byte[] body, Map<String, String> extra, String operationId) {
        String correlationId = newCorrelationId();
        String url = options.baseUrl().replaceAll("/+$", "") + path;
        Map<String, String> headers = standardHeaders(correlationId, null);
        headers.putAll(extra);
        headers.put("Content-Type", "application/json; charset=utf-8");
        TransynqClientOptions.RequestInfo info = new TransynqClientOptions.RequestInfo("POST", url, operationId, 1, redact(headers));
        if (options.onRequest() != null) {
            options.onRequest().accept(info);
        }
        long started = System.nanoTime();
        HttpResponse<InputStream> response;
        try {
            response = exchange("POST", url, headers, HttpRequest.BodyPublishers.ofByteArray(body), options.timeout());
        } catch (HttpTimeoutException e) {
            throw new TransynqTimeoutException(operationId + " timed out [correlation id " + correlationId + "]", e);
        } catch (IOException e) {
            throw new TransynqConnectionException(operationId + " could not reach the API: " + e + " [correlation id " + correlationId + "]", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransynqException(operationId + " was interrupted", e);
        }
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        log(operationId, 1, response.statusCode(), elapsedMs, correlationId);
        Map<String, String> responseHeaders = headersOf(response);
        if (options.onResponse() != null) {
            options.onResponse().accept(new TransynqClientOptions.ResponseInfo(info, response.statusCode(), Duration.ofMillis(elapsedMs), redact(responseHeaders)));
        }
        byte[] bytes = readAll(response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw ApiException.fromResponse(response.statusCode(), bytes, responseHeaders, operationId, retryAfter(responseHeaders.get("retry-after")));
        }
        List<String> cookies = response.headers().allValues("set-cookie");
        return new CredentialRequest.SignInResponse(response.statusCode(), bytes, cookies);
    }
}
