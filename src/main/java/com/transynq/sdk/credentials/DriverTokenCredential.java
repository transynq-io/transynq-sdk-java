package com.transynq.sdk.credentials;

/** An existing driver-app token ({@code Authorization: Bearer <token>}), valid for {@code /api/v1/driver/*}. */
public final class DriverTokenCredential implements Credential {
    private final String token;

    /**
     * Creates the credential.
     *
     * @param token the token issued by {@code POST /api/v1/driver/login}
     */
    public DriverTokenCredential(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token is required");
        }
        this.token = token;
    }

    @Override
    public void apply(CredentialRequest request) {
        request.setHeader("Authorization", "Bearer " + token);
    }

    @Override
    public String toString() {
        return "DriverTokenCredential([redacted])";
    }
}
