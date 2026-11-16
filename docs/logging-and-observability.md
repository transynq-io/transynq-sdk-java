# Logging and observability

```java
// debug lines go to System.Logger "io.transynq.sdk" (bridged to SLF4J/Log4j/JUL by your logging setup)
TransynqClient client = new TransynqClient(TransynqClientOptions.builder("https://api.example.com")
        .credential(new ApiKeyCredential(apiKey))
        .onRequest(info -> System.out.println("-> " + info.method() + " " + info.operationId() + " #" + info.attempt()))
        .onResponse(info -> System.out.println("<- " + info.statusCode() + " in " + info.elapsed().toMillis() + " ms"))
        .build());
```

- The logger receives one debug line per attempt: operation id, attempt number, status (or `timeout` / network error),
  duration and correlation id. Never request or response bodies, never credentials.
- The request hook runs before every attempt (method, URL, operation id, attempt, headers); the response hook after every
  answer (status, elapsed time, headers). Credential headers are `[redacted]` in both.
- Every request carries `User-Agent: transynq-sdk-java/1.0.0` (plus your suffix) and a client-generated `X-Correlation-Id`
  (32 hex characters, the same on every retry of one call). The API logs under that id and echoes it back.

## On every response

`ApiResponse<T>` exposes `data()`, `statusCode()`, `headers()`, `etag()`, `apiVersion()` (`X-Api-Version`), `correlationId()` (`X-Correlation-Id`), `location()` and `isAccepted()`.

`X-Api-Version` is the version of the contract that answered; log it next to your own version to diagnose a mismatch after an
API release.
