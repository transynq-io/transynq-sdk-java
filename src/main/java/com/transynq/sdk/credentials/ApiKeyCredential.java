package com.transynq.sdk.credentials;

/** An API client key, sent as {@code X-Transynq-APIKey}. The key's company is the tenant; no cookie, no CSRF. */
public final class ApiKeyCredential implements Credential {
    /** The header name. */
    public static final String HEADER = "X-Transynq-APIKey";

    private final String key;

    /**
     * Creates the credential.
     *
     * @param apiKey the key (keep it in a secret store)
     */
    public ApiKeyCredential(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey is required");
        }
        this.key = apiKey;
    }

    @Override
    public void apply(CredentialRequest request) {
        request.setHeader(HEADER, key);
    }

    @Override
    public String toString() {
        return "ApiKeyCredential([redacted])";
    }
}
