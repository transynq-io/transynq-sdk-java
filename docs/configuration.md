# Configuration

## Base URL, timeouts and retries

```java
TransynqClient client = new TransynqClient(TransynqClientOptions.builder("https://api.example.com")
        .credential(new ApiKeyCredential(apiKey))
        .timeout(Duration.ofSeconds(30))          // per attempt (default 60 s)
        .maxRetries(3)                            // default 2; 0 turns retries off
        .retryBaseDelay(Duration.ofMillis(250))
        .maxRetryDelay(Duration.ofSeconds(5))
        .maxRetryAfter(Duration.ofSeconds(30))
        .userAgentSuffix("acme-erp-sync/2.1")     // User-Agent: transynq-sdk-java/1.0.0 acme-erp-sync/2.1
        .executor(Executors.newFixedThreadPool(8)) // runs the ...Async methods (default: the common pool)
        .build());

// per call
client.referenceData().listCountries(new RequestOptions(null, null, null, Duration.ofSeconds(5), 0, null));
```

| Setting | Default | Meaning |
|---|---|---|
| base URL | required | The API host, for example `https://api.example.com`; paths such as `/api/v1/jobs` are appended. |
| credential | none | See [Authentication](authentication.md). |
| timeout | 60 s | Per attempt (sending and reading a buffered answer). |
| max retries | 2 | Retries after the first attempt for retryable failures; 0 turns retries off. |
| retry base delay | 0.5 s | First back-off delay, doubled per attempt, with full jitter. |
| max retry delay | 8 s | Cap of a computed back-off delay. |
| max Retry-After | 60 s | A longer `Retry-After` is not waited for: the rate-limit error is raised. |
| user agent suffix | none | Appended to `User-Agent: transynq-sdk-java/1.0.0`. |
| hooks / logger | none | See [Logging and observability](logging-and-observability.md). |

Per-call `RequestOptions` override the timeout and retries and add `If-Match`, `Idempotency-Key`, extra headers and a
correlation id.

## Retries

Retried automatically (default `2` retries after the first attempt):

- network failures and timeouts, and the statuses 408, 429, 500, 502, 503 and 504 (never 501 or any other 4xx);
- only when repeating the request is safe: `GET`, `HEAD`, `OPTIONS`, `PUT`, `DELETE`; any request that carries an `Idempotency-Key`;
  a create whose JSON body carries a client-generated, non-empty `id`. A 429 is always retried (the rate limiter refuses before
  the endpoint runs);
- the wait is exponential with full jitter (0.5 s, 1 s, 2 s... capped at 8 s), or the server's `Retry-After` when it sends one.
  A `Retry-After` longer than 60 s is not waited for: the rate-limit error is raised so you can schedule the work;
- every attempt of one call carries the same `X-Correlation-Id`, so the server logs show one call;
- a 401 with a credential that can sign in again (session, driver login) is retried once after signing in, independently of the
  retry count.

## Custom HTTP client and proxies

```java
// your own java.net.http.HttpClient (proxy, TLS, HTTP/2). Do not give it a CookieHandler: the SDK keeps the session cookie itself.
HttpClient http = HttpClient.newBuilder()
        .proxy(ProxySelector.of(new InetSocketAddress("proxy.example.com", 8080)))
        .connectTimeout(Duration.ofSeconds(10))
        .build();
TransynqClient client = new TransynqClient(TransynqClientOptions.builder("https://api.example.com")
        .credential(new ApiKeyCredential(apiKey))
        .httpClient(http)
        .build());
```

## Several companies or environments

Create one client per base URL and credential and keep it for the life of your process; clients are thread-safe. Two
credentials need two clients.
