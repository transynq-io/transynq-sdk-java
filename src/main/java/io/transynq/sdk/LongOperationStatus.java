package io.transynq.sdk;

/** The states of a long operation (an open set: compare as strings, ignoring case). */
public final class LongOperationStatus {
    /** Waiting to start. */
    public static final String QUEUED = "Queued";
    /** Running. */
    public static final String RUNNING = "Running";
    /** Finished; {@code result} holds the outcome. */
    public static final String SUCCEEDED = "Succeeded";
    /** Finished; {@code error} says why. */
    public static final String FAILED = "Failed";

    private LongOperationStatus() {
    }
}
