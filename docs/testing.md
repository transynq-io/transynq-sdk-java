# Testing your integration

Unit tests should not call the real API. Answer the SDK's requests in memory:

```java
// Answer requests from an in-process server on the loopback interface (no external network); JDK only.
HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
server.createContext("/api/v1/countries", exchange -> {
    byte[] body = "[{\"id\":1,\"name\":\"Australia\"}]".getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
});
server.start();

TransynqClient client = new TransynqClient(TransynqClientOptions
        .builder("http://127.0.0.1:" + server.getAddress().getPort())
        .credential(new ApiKeyCredential("test-key"))
        .build());
assertEquals("Australia", client.referenceData().listCountries().data().get(0).name());
server.stop(0);
```

Point the client at a local mock server (the JDK `HttpServer` as above, which is how the SDK's own tests work, or WireMock / MockWebServer), or wrap the calls your code makes behind an interface of yours and mock that with Mockito.

Tips:

- Never put real API keys, passwords, hostnames or customer data in tests; use fake values such as `test-key` and
  `https://api.example.com`.
- Answer error cases with ProblemDetails bodies (`{"title": "...", "status": 422, "code": "id-in-use"}`) to exercise your error
  handling; the SDK maps them to the same typed errors as in production.
- Set the client's max retries to 0 in tests that answer 5xx, or retries will consume your queued answers.
