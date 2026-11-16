package io.transynq.sdk.credentials;

import java.util.List;
import java.util.Map;

/** A request as a credential sees it. */
public final class CredentialRequest {
    /**
     * A successful sign-in answer.
     *
     * @param statusCode the status
     * @param body the body
     * @param setCookies the {@code Set-Cookie} values
     */
    public record SignInResponse(int statusCode, byte[] body, List<String> setCookies) {
    }

    /** Sends sign-in calls for credentials: no credential, no retries, errors mapped to exceptions. */
    @FunctionalInterface
    public interface SignInTransport {
        /**
         * POSTs JSON.
         *
         * @param path the API path
         * @param body the JSON
         * @param headers extra headers
         * @param operationId the name for logs and errors
         * @return the answer
         */
        SignInResponse postJson(String path, byte[] body, Map<String, String> headers, String operationId);
    }

    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final SignInTransport transport;
    private Object appliedState;

    /**
     * Used by the pipeline.
     *
     * @param method the method
     * @param url the URL
     * @param headers the mutable headers
     * @param transport the sign-in transport
     */
    public CredentialRequest(String method, String url, Map<String, String> headers, SignInTransport transport) {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.transport = transport;
    }

    /** @return the HTTP method */
    public String method() {
        return method;
    }

    /** @return the URL */
    public String url() {
        return url;
    }

    /** @return true for POST, PUT, PATCH and DELETE */
    public boolean isUnsafe() {
        return !(method.equals("GET") || method.equals("HEAD") || method.equals("OPTIONS"));
    }

    /** @return the sign-in transport */
    public SignInTransport transport() {
        return transport;
    }

    /** @return what the credential applied, or null */
    public Object appliedState() {
        return appliedState;
    }

    /**
     * Records what the credential applied (compared on a 401).
     *
     * @param state the state
     */
    public void appliedState(Object state) {
        this.appliedState = state;
    }

    /**
     * Sets (replaces) a header.
     *
     * @param name the name
     * @param value the value
     */
    public void setHeader(String name, String value) {
        headers.keySet().removeIf(k -> k.equalsIgnoreCase(name));
        headers.put(name, value);
    }
}
