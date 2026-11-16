package io.transynq.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.transynq.sdk.credentials.DriverLoginCredential;
import io.transynq.sdk.credentials.SessionCredential;
import io.transynq.sdk.internal.HttpPipeline;
import io.transynq.sdk.models.GridPageResponse;
import io.transynq.sdk.models.GridQueryRequest;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * The Transynq TMS API client. One accessor per API area ({@code client.jobs()}, {@code client.customers()}...), one method per
 * operation, each with a {@code CompletableFuture} twin. Thread-safe; create one per base URL and credential and reuse it.
 */
public final class TransynqClient extends TransynqServices {
    private final TransynqClientOptions options;
    private final HttpPipeline pipeline;

    /**
     * Creates a client.
     *
     * @param options the settings
     */
    public TransynqClient(TransynqClientOptions options) {
        this(new HttpPipeline(options));
    }

    private TransynqClient(HttpPipeline pipeline) {
        super(pipeline);
        this.pipeline = pipeline;
        this.options = pipeline.options();
    }

    /** @return the settings */
    public TransynqClientOptions options() {
        return options;
    }

    /** @return the {@code User-Agent} this client sends */
    public String userAgent() {
        return pipeline.userAgent();
    }

    /**
     * A poller for the long operation a 202 answer started (its {@code Location}, else its body's {@code id}).
     *
     * @param accepted the 202 answer
     * @return the poller
     */
    public LongOperationPoller pollLongOperation(ApiResponse<ResponseBody> accepted) {
        String location = accepted.location();
        if (location != null && location.toLowerCase(java.util.Locale.ROOT).contains("/long-operations/")) {
            String path = location.split("\\?", 2)[0].replaceAll("/+$", "");
            return new LongOperationPoller(this, UUID.fromString(path.substring(path.lastIndexOf('/') + 1)));
        }
        JsonNode body = accepted.data().json();
        if (body != null && body.hasNonNull("id")) {
            return new LongOperationPoller(this, UUID.fromString(body.get("id").asText()));
        }
        throw new IllegalArgumentException("The response is not a long-operation answer (no Location and no id).");
    }

    /**
     * A poller for a long operation by id.
     *
     * @param operationId the id
     * @return the poller
     */
    public LongOperationPoller pollLongOperation(UUID operationId) {
        return new LongOperationPoller(this, operationId);
    }

    /**
     * Every row of {@code POST /api/v1/grids/{gridId}/query}, read lazily page by page (page size default 500, the maximum).
     *
     * @param gridId the grid
     * @param request columns, filters, sort, search
     * @param callOptions per-call options, or null
     * @return the rows, as JSON objects keyed by column
     */
    public Iterable<ObjectNode> gridRows(String gridId, GridQueryRequest request, RequestOptions callOptions) {
        int start = request.page() != null ? request.page() : 1;
        int size = request.pageSize() != null ? request.pageSize() : 500;
        return () -> new Iterator<ObjectNode>() {
            private int page = start;
            private Iterator<ObjectNode> current;
            private boolean last;

            @Override
            public boolean hasNext() {
                while (current == null || !current.hasNext()) {
                    if (last) {
                        return false;
                    }
                    GridPageResponse data = grids().queryGrid(gridId, request.toBuilder().page(page).pageSize(size).build(), callOptions).data();
                    if (data == null || data.rows() == null || data.rows().isEmpty()) {
                        last = true;
                        return false;
                    }
                    current = data.rows().iterator();
                    last = (long) data.page() * data.pageSize() >= data.total();
                    page++;
                }
                return true;
            }

            @Override
            public ObjectNode next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return current.next();
            }
        };
    }

    /** Ends a session or driver sign-in on the server and forgets it locally. */
    public void signOut() {
        if (options.credential() instanceof SessionCredential session && session.state() != null) {
            authentication().logout();
            session.reset();
        } else if (options.credential() instanceof DriverLoginCredential driver && driver.state() != null) {
            driverApp().driverLogout();
            driver.reset();
        }
    }

    HttpPipeline pipeline() {
        return pipeline;
    }
}
