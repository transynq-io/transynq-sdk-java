package com.transynq.sdk.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.transynq.sdk.ApiResponse;
import com.transynq.sdk.Concurrency;
import com.transynq.sdk.FileUpload;
import com.transynq.sdk.Ids;
import com.transynq.sdk.LongOperationPoller;
import com.transynq.sdk.Paging;
import com.transynq.sdk.ResponseBody;
import com.transynq.sdk.TransynqClient;
import com.transynq.sdk.TransynqClientOptions;
import com.transynq.sdk.credentials.ApiKeyCredential;
import com.transynq.sdk.credentials.Credential;
import com.transynq.sdk.credentials.SessionCredential;
import com.transynq.sdk.errors.LongOperationFailedException;
import com.transynq.sdk.models.BookingRequestDto;
import com.transynq.sdk.models.GridQueryRequest;
import com.transynq.sdk.models.GridSummaryResponse;
import com.transynq.sdk.models.JobPodsAddedResponse;
import com.transynq.sdk.models.LongOperationResponse;
import com.transynq.sdk.models.PublicHolidayRequest;
import com.transynq.sdk.models.PublicHolidayResponse;
import com.transynq.sdk.requests.AddJobPodsForm;
import com.transynq.sdk.requests.GetSalesLedgerInvoicesQuery;
import com.transynq.sdk.requests.ImportZonesInBackgroundForm;
import com.transynq.sdk.requests.ListJobStatusReasonsQuery;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Runnable samples of real Transynq flows.
 *
 * <p>Configure with environment variables (never hard-code secrets): {@code TRANSYNQ_BASE_URL} (e.g. https://api.example.com),
 * {@code TRANSYNQ_API_KEY}, or {@code TRANSYNQ_USER} / {@code TRANSYNQ_PASSWORD}.
 *
 * <p>Flows: sign-in | page-grid | create-booking | update-holiday | list-invoices | upload-pod | long-operation
 */
public final class Flows {
    private Flows() {
    }

    static TransynqClient createClient() {
        String key = System.getenv("TRANSYNQ_API_KEY");
        Credential credential = key != null && !key.isEmpty()
                ? new ApiKeyCredential(key)
                : new SessionCredential(env("TRANSYNQ_USER", "user@example.com"), env("TRANSYNQ_PASSWORD", "change-me"));
        return new TransynqClient(TransynqClientOptions.builder(env("TRANSYNQ_BASE_URL", "https://api.example.com"))
                .credential(credential)
                .userAgentSuffix("transynq-examples/1.0")
                .build());
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isEmpty() ? fallback : value;
    }

    /** Sign in (session or API key) and show who the client acts as. */
    static void signIn(TransynqClient client) {
        if (client.options().credential() instanceof SessionCredential) {
            ApiResponse<ResponseBody> me = client.authentication().getMe();
            JsonNode profile = me.data().json();
            System.out.println("Signed in as " + profile.get("userName").asText() + " of " + profile.get("companyName").asText()
                    + " (API " + me.apiVersion() + ").");
            client.signOut();
        } else {
            System.out.println("API key acts for company " + client.authentication().getApiClient().data().json().get("companyId").asText());
        }
    }

    /** List the grids, then read every row of one (jobs are listed through the grid endpoints). */
    static void pageGrid(TransynqClient client, String gridId) {
        for (GridSummaryResponse grid : client.grids().listGrids().data()) {
            System.out.println("grid " + grid.id() + ": " + grid.title());
        }
        int count = 0;
        for (ObjectNode row : client.gridRows(gridId, GridQueryRequest.builder().pageSize(200).build(), null)) {
            if (++count <= 5) {
                System.out.println(row);
            }
        }
        System.out.println(count + " rows");
        // a classic paged collection with auto-paging, as a Stream
        Paging.stream(client.referenceData().listJobStatusReasonsAutoPaging(ListJobStatusReasonsQuery.builder().activeOnly(true).pageSize(100).build(), null))
                .forEach(r -> System.out.println(r.reasonType() + ": " + r.name()));
    }

    /** Create a job (booking) idempotently: the client-generated id makes a retry return the same record. */
    static void createBooking(TransynqClient client, UUID customerId, UUID senderId, UUID recipientId) {
        BookingRequestDto booking = BookingRequestDto.builder()
                .id(Ids.newId())
                .customerId(customerId)
                .senderId(senderId)
                .recipientId(recipientId)
                .bookDate(OffsetDateTime.now(ZoneOffset.UTC))
                .invoicingOnly(false)
                .specialInstructions("Call before delivery")
                .products(List.of())
                .legs(List.of())
                .pricings(List.of())
                .notes(List.of())
                .build();
        ApiResponse<ResponseBody> created = client.bookings().createBooking(booking);
        System.out.println("Booking " + booking.id() + " saved (" + created.statusCode() + "); correlation id " + created.correlationId() + ".");
    }

    /** Update with optimistic concurrency: read the ETag, send it as If-Match, retry on a stale version. */
    static void updateHoliday(TransynqClient client, UUID holidayId) {
        ApiResponse<PublicHolidayResponse> saved = Concurrency.readModifyWrite(
                () -> client.referenceData().getPublicHoliday(holidayId),
                (current, options) -> client.referenceData().updatePublicHoliday(holidayId, PublicHolidayRequest.builder()
                        .id(current.id())
                        .name(current.name().trim())
                        .date(current.date())
                        .countryId(current.countryId())
                        .states(current.states())
                        .build(), options),
                3);
        System.out.println("Saved; new ETag " + saved.etag() + ".");
    }

    /** List sales-ledger invoices of the last 30 days. */
    static void listInvoices(TransynqClient client) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        ApiResponse<ResponseBody> response = client.salesLedger().getSalesLedgerInvoices(
                GetSalesLedgerInvoicesQuery.builder().from(now.minusDays(30)).to(now).take(50).build());
        System.out.println(response.data().json());
    }

    /** Upload a proof of delivery to a job. */
    static void uploadPod(TransynqClient client, UUID jobId, Path path) {
        ApiResponse<JobPodsAddedResponse> added = client.jobs().addJobPods(jobId,
                AddJobPodsForm.builder().files(List.of(FileUpload.fromPath(path, "application/pdf"))).build(), null);
        System.out.println("Added POD(s): " + added.data().ids());
    }

    /** Queue a background import and wait for the long operation to finish. */
    static void longOperation(TransynqClient client, Path workbook) {
        ApiResponse<ResponseBody> accepted = client.imports().importZonesInBackground(ImportZonesInBackgroundForm.builder()
                .file(FileUpload.fromPath(workbook, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .build());
        LongOperationPoller poller = client.pollLongOperation(accepted);
        try {
            LongOperationResponse done = poller.waitForCompletion(Duration.ofSeconds(2), Duration.ofMinutes(30),
                    op -> System.out.println(op.kind() + ": " + op.status()));
            System.out.println("Result: " + done.result());
        } catch (LongOperationFailedException failed) {
            System.err.println("Import failed: " + failed.operation().error());
        }
    }

    /**
     * Entry point.
     *
     * @param args the flow and its arguments
     */
    public static void main(String[] args) {
        String flow = args.length > 0 ? args[0] : "sign-in";
        TransynqClient client = createClient();
        switch (flow) {
            case "sign-in" -> signIn(client);
            case "page-grid" -> pageGrid(client, args.length > 1 ? args[1] : "jobs");
            case "create-booking" -> createBooking(client, UUID.fromString(args[1]), UUID.fromString(args[2]), UUID.fromString(args[3]));
            case "update-holiday" -> updateHoliday(client, UUID.fromString(args[1]));
            case "list-invoices" -> listInvoices(client);
            case "upload-pod" -> uploadPod(client, UUID.fromString(args[1]), Path.of(args[2]));
            case "long-operation" -> longOperation(client, Path.of(args[1]));
            default -> {
                System.err.println("Unknown flow '" + flow + "'.");
                System.exit(1);
            }
        }
    }
}
