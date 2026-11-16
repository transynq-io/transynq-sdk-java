package com.transynq.sdk.credentials;

import com.transynq.sdk.errors.ApiException;
import com.transynq.sdk.errors.AuthenticationException;
import com.transynq.sdk.internal.Json;
import com.transynq.sdk.models.DriverLoginRequest;
import com.transynq.sdk.models.DriverSession;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Driver-app sign-in ({@code POST /api/v1/driver/login}), then {@code Authorization: Bearer <token>}. */
public final class DriverLoginCredential extends SignInCredential<DriverLoginCredential.DriverToken> {
    /**
     * A driver token (never printed).
     *
     * @param token the bearer token
     * @param expiresAt when it ends, or null
     * @param session the login answer
     */
    public record DriverToken(String token, Instant expiresAt, DriverSession session) {
        @Override
        public String toString() {
            return "DriverToken(expiresAt=" + expiresAt + ", [redacted])";
        }
    }

    private final DriverLoginRequest login;

    /**
     * Creates the credential (nothing is sent until the first request).
     *
     * @param login company code, PIN, device id, push token, app version and platform
     */
    public DriverLoginCredential(DriverLoginRequest login) {
        this.login = Objects.requireNonNull(login, "login");
    }

    /** @return the last driver session, or null */
    public DriverSession session() {
        DriverToken t = state();
        return t == null ? null : t.session();
    }

    @Override
    protected DriverToken signIn(CredentialRequest.SignInTransport transport) {
        try {
            CredentialRequest.SignInResponse answer = transport.postJson("/api/v1/driver/login", Json.mapper().writeValueAsBytes(login), Map.of(), "DriverLogin");
            DriverSession session = Json.mapper().readValue(answer.body(), DriverSession.class);
            if (session == null || session.token() == null || session.token().isEmpty()) {
                String m = "DriverLogin answered without a token.";
                throw new AuthenticationException(m, new ApiException.Details(answer.statusCode(), null, m, null, null, null, "", Map.of(), "DriverLogin"));
            }
            return new DriverToken(session.token(), session.expiresUtc() == null ? null : session.expiresUtc().toInstant(), session);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected Instant expiresAt(DriverToken state) {
        return state.expiresAt();
    }

    @Override
    protected void applyState(CredentialRequest request, DriverToken state) {
        request.setHeader("Authorization", "Bearer " + state.token());
    }

    @Override
    public String toString() {
        return "DriverLoginCredential([redacted])";
    }
}
