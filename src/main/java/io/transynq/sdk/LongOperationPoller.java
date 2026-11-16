package io.transynq.sdk;

import io.transynq.sdk.errors.LongOperationFailedException;
import io.transynq.sdk.errors.TransynqException;
import io.transynq.sdk.models.LongOperationResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Follows a long-running operation ({@code GET /api/v1/long-operations/{id}}) to its end. */
public final class LongOperationPoller {
    private final TransynqClient client;
    private final UUID id;
    private volatile LongOperationResponse last;

    LongOperationPoller(TransynqClient client, UUID id) {
        this.client = client;
        this.id = id;
    }

    /** @return the operation id */
    public UUID id() {
        return id;
    }

    /** @return the last state read, or null */
    public LongOperationResponse last() {
        return last;
    }

    /** @return the current state, read once */
    public LongOperationResponse status() {
        last = client.longOperations().getLongOperation(id).data();
        return last;
    }

    /**
     * Polls every 2 s for up to 30 minutes.
     *
     * @return the succeeded operation
     */
    public LongOperationResponse waitForCompletion() {
        return waitForCompletion(Duration.ofSeconds(2), Duration.ofMinutes(30), null);
    }

    /**
     * Polls until {@code Succeeded} (returns it) or {@code Failed} (throws {@link LongOperationFailedException}).
     *
     * @param pollInterval the time between polls
     * @param timeout gives up after this long
     * @param onProgress called with every state read; may be null
     * @return the succeeded operation (its {@code result} holds the outcome)
     */
    public LongOperationResponse waitForCompletion(Duration pollInterval, Duration timeout, Consumer<LongOperationResponse> onProgress) {
        Duration waited = Duration.ZERO;
        while (true) {
            LongOperationResponse op = status();
            if (onProgress != null) {
                onProgress.accept(op);
            }
            String s = op.status() == null ? "" : op.status().toLowerCase(Locale.ROOT);
            if (s.equals("succeeded")) {
                return op;
            }
            if (s.equals("failed")) {
                throw new LongOperationFailedException("Long operation " + id + " (" + op.kind() + ") failed: "
                        + (op.error() != null ? op.error() : "no reason given") + ".", op);
            }
            if (waited.compareTo(timeout) >= 0) {
                throw new TransynqException("Long operation " + id + " did not finish within " + timeout + ".");
            }
            try {
                client.options().sleeper().sleep(pollInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TransynqException("interrupted while waiting for long operation " + id, e);
            }
            waited = waited.plus(pollInterval);
        }
    }

    /**
     * {@link #waitForCompletion(Duration, Duration, Consumer)} on the client's executor.
     *
     * @param pollInterval the time between polls
     * @param timeout gives up after this long
     * @param onProgress called with every state read; may be null
     * @return the future operation
     */
    public CompletableFuture<LongOperationResponse> waitAsync(Duration pollInterval, Duration timeout, Consumer<LongOperationResponse> onProgress) {
        return client.pipeline().async(() -> waitForCompletion(pollInterval, timeout, onProgress));
    }

    /** @return the operation's result file (a workbook or PDF); close it */
    public FileDownload downloadResult() {
        return client.longOperations().getLongOperationFile(id).data();
    }
}
