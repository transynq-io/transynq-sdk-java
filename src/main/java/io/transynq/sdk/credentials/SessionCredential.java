package io.transynq.sdk.credentials;

import com.fasterxml.jackson.databind.JsonNode;
import io.transynq.sdk.errors.ApiException;
import io.transynq.sdk.errors.AuthenticationException;
import io.transynq.sdk.internal.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A user session: {@code POST /api/v1/auth/login} with user name and password, then the session cookie on every request and
 * {@code X-CSRF-Token} on every unsafe one. Signs in again before the session expires and once after a 401.
 */
public final class SessionCredential extends SignInCredential<SessionCredential.Session> {
    /** The CSRF header name. */
    public static final String CSRF_HEADER = "X-CSRF-Token";

    /**
     * A signed-in session (never printed with its secrets).
     *
     * @param cookie the session cookie pair
     * @param csrfToken the CSRF token bound to the session
     * @param expiresAt the absolute end of the session, or null
     * @param me the login answer ({@code MeResponse})
     */
    public record Session(String cookie, String csrfToken, Instant expiresAt, JsonNode me) {
        @Override
        public String toString() {
            return "Session(expiresAt=" + expiresAt + ", [redacted])";
        }
    }

    private final String userName;
    private final String password;

    /**
     * Creates the credential (nothing is sent until the first request).
     *
     * @param userName the user name
     * @param password the password (keep it in a secret store)
     */
    public SessionCredential(String userName, String password) {
        this.userName = Objects.requireNonNull(userName, "userName");
        this.password = Objects.requireNonNull(password, "password");
    }

    /** @return the login answer, or null before the first request */
    public JsonNode me() {
        Session s = state();
        return s == null ? null : s.me();
    }

    /**
     * The {@code name=value} of the session cookie ({@code tq_session} or {@code __Host-tq_session}).
     *
     * @param setCookies the {@code Set-Cookie} values
     * @return the pair, or null
     */
    public static String sessionCookie(List<String> setCookies) {
        for (String header : setCookies) {
            String pair = header.split(";", 2)[0].trim();
            int eq = pair.indexOf('=');
            if (eq > 0 && eq < pair.length() - 1 && pair.substring(0, eq).endsWith("tq_session")) {
                return pair;
            }
        }
        return null;
    }

    private static AuthenticationException fail(String message, int status) {
        return new AuthenticationException(message, new ApiException.Details(status, null, message, null, null, null, "", Map.of(), "Login"));
    }

    @Override
    protected Session signIn(CredentialRequest.SignInTransport transport) {
        byte[] body;
        try {
            body = Json.mapper().writeValueAsBytes(Map.of("userName", userName, "password", password));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // without a session the CSRF check only needs a non-empty value
        CredentialRequest.SignInResponse answer = transport.postJson("/api/v1/auth/login", body, Map.of(CSRF_HEADER, "transynq-sdk"), "Login");
        String cookie = sessionCookie(answer.setCookies());
        if (cookie == null) {
            throw fail("Login answered without a session cookie.", answer.statusCode());
        }
        JsonNode me;
        try {
            me = Json.mapper().readTree(answer.body());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        JsonNode csrf = me == null ? null : me.get("csrfToken");
        if (csrf == null || !csrf.isTextual() || csrf.asText().isEmpty()) {
            throw fail("Login answered without a csrfToken.", answer.statusCode());
        }
        JsonNode expires = me.get("sessionExpiresUtc");
        Instant at = expires != null && expires.isTextual() ? Json.parseInstant(expires.asText()).toInstant() : null;
        return new Session(cookie, csrf.asText(), at, me);
    }

    @Override
    protected Instant expiresAt(Session state) {
        return state.expiresAt();
    }

    @Override
    protected void applyState(CredentialRequest request, Session state) {
        request.setHeader("Cookie", state.cookie());
        if (request.isUnsafe()) {
            request.setHeader(CSRF_HEADER, state.csrfToken());
        }
    }

    @Override
    public String toString() {
        return "SessionCredential(" + userName + ", [redacted])";
    }
}
