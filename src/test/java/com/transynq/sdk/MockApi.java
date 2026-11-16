package com.transynq.sdk;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** An in-process HTTP server on the loopback interface that answers from a queue and records requests. No external network. */
final class MockApi implements AutoCloseable {
    record Recorded(String method, String path, String query, Map<String, String> headers, byte[] body) {
        String header(String name) {
            return headers.get(name.toLowerCase(java.util.Locale.ROOT));
        }

        String bodyText() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    record Answer(int status, String body, Map<String, String> headers) {
        static Answer json(int status, String body, String... headerPairs) {
            Map<String, String> h = new LinkedHashMap<>();
            h.put("Content-Type", status >= 400 ? "application/problem+json" : "application/json");
            for (int i = 0; i < headerPairs.length; i += 2) {
                h.put(headerPairs[i], headerPairs[i + 1]);
            }
            return new Answer(status, body, h);
        }

        static Answer empty(int status, String... headerPairs) {
            Map<String, String> h = new LinkedHashMap<>();
            for (int i = 0; i < headerPairs.length; i += 2) {
                h.put(headerPairs[i], headerPairs[i + 1]);
            }
            return new Answer(status, null, h);
        }
    }

    private final HttpServer server;
    private final Deque<Function<Recorded, Answer>> answers = new ArrayDeque<>();
    final List<Recorded> requests = Collections.synchronizedList(new ArrayList<>());
    volatile Function<Recorded, Answer> fallback;

    MockApi() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    MockApi enqueue(Answer answer) {
        synchronized (answers) {
            answers.add(r -> answer);
        }
        return this;
    }

    MockApi enqueue(Function<Recorded, Answer> answer) {
        synchronized (answers) {
            answers.add(answer);
        }
        return this;
    }

    private void handle(HttpExchange exchange) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        exchange.getRequestHeaders().forEach((k, v) -> headers.put(k.toLowerCase(java.util.Locale.ROOT), String.join(", ", v)));
        byte[] body = exchange.getRequestBody().readAllBytes();
        Recorded r = new Recorded(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(), exchange.getRequestURI().getRawQuery(), headers, body);
        requests.add(r);
        Function<Recorded, Answer> f;
        synchronized (answers) {
            f = answers.isEmpty() ? fallback : answers.poll();
        }
        Answer a = f == null ? Answer.json(599, "{\"title\":\"no answer queued\"}") : f.apply(r);
        a.headers().forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
        byte[] out = a.body() == null ? new byte[0] : a.body().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(a.status(), out.length == 0 ? -1 : out.length);
        if (out.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            }
        }
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
