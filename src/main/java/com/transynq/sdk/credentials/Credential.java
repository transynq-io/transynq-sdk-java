package com.transynq.sdk.credentials;

/** How requests authenticate (docs/design.md section 3). Implementations must be thread-safe. */
public interface Credential {
    /**
     * Adds headers before a request is sent, signing in first when needed.
     *
     * @param request the request
     */
    void apply(CredentialRequest request);

    /**
     * Called on a 401.
     *
     * @param request the request that failed
     * @return true when one retry with fresh credentials is worthwhile
     */
    default boolean refreshAfterUnauthorized(CredentialRequest request) {
        return false;
    }
}
