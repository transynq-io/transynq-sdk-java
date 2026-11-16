package io.transynq.sdk.credentials;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Base of credentials that sign in: single-flight sign-in, refresh before expiry and once after a 401.
 *
 * @param <S> what a sign-in yields
 */
public abstract class SignInCredential<S> implements Credential {
    private final Object lock = new Object();
    private volatile S state;
    private volatile Supplier<Instant> clock = Instant::now;
    private volatile int signInCount;
    private Duration refreshMargin = Duration.ofSeconds(60);

    /** Creates the base. */
    protected SignInCredential() {
    }

    /** @return the current signed-in state, or null */
    public S state() {
        return state;
    }

    /** @return the number of sign-ins performed (diagnostics) */
    public int signInCount() {
        return signInCount;
    }

    /** Forgets the signed-in state (after a sign-out); the next request signs in again. */
    public void reset() {
        state = null;
    }

    /**
     * Replaces the clock (tests).
     *
     * @param value the clock
     */
    public void clock(Supplier<Instant> value) {
        this.clock = value;
    }

    /**
     * Sets how long before expiry to sign in again (default 60 s).
     *
     * @param value the margin
     */
    public void refreshMargin(Duration value) {
        this.refreshMargin = value;
    }

    private boolean fresh(S s) {
        if (s == null) {
            return false;
        }
        Instant expires = expiresAt(s);
        return expires == null || expires.minus(refreshMargin).isAfter(clock.get());
    }

    @Override
    public final void apply(CredentialRequest request) {
        S s = state;
        if (!fresh(s)) {
            synchronized (lock) {
                s = state;
                if (!fresh(s)) {
                    s = signIn(request.transport());
                    signInCount++;
                    state = s;
                }
            }
        }
        request.appliedState(s);
        applyState(request, s);
    }

    @Override
    public final boolean refreshAfterUnauthorized(CredentialRequest request) {
        synchronized (lock) {
            // only drop the state the failed request used; another caller may already have signed in again
            if (state == request.appliedState()) {
                state = null;
            }
        }
        return true;
    }

    /**
     * Signs in.
     *
     * @param transport sends the sign-in call
     * @return the new state
     */
    protected abstract S signIn(CredentialRequest.SignInTransport transport);

    /**
     * When the state stops being valid.
     *
     * @param state the state
     * @return the expiry, or null when unknown
     */
    protected abstract Instant expiresAt(S state);

    /**
     * Adds the state's headers.
     *
     * @param request the request
     * @param state the state
     */
    protected abstract void applyState(CredentialRequest request, S state);
}
