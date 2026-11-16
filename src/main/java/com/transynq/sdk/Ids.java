package com.transynq.sdk;

import java.util.UUID;

/** Client-generated identifiers. */
public final class Ids {
    private Ids() {
    }

    /** @return a new id for an idempotent create ({@code POST} with {@code id} in the body: sending it again returns the same record) */
    public static UUID newId() {
        return UUID.randomUUID();
    }

    /** @return a new {@code Idempotency-Key} value (32 hex characters) */
    public static String newIdempotencyKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
