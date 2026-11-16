package com.transynq.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.transynq.sdk.MockApi.Answer;
import com.transynq.sdk.credentials.ApiKeyCredential;
import com.transynq.sdk.credentials.DriverLoginCredential;
import com.transynq.sdk.credentials.SessionCredential;
import com.transynq.sdk.errors.ApiException;
import com.transynq.sdk.errors.AuthenticationException;
import com.transynq.sdk.errors.BusinessRuleException;
import com.transynq.sdk.errors.ConflictException;
import com.transynq.sdk.errors.LongOperationFailedException;
import com.transynq.sdk.errors.NotFoundException;
import com.transynq.sdk.errors.PermissionDeniedException;
import com.transynq.sdk.errors.RateLimitException;
import com.transynq.sdk.errors.ServerException;
import com.transynq.sdk.errors.StaleVersionException;
import com.transynq.sdk.errors.TransynqConnectionException;
import com.transynq.sdk.errors.TransynqTimeoutException;
import com.transynq.sdk.errors.ValidationException;
import com.transynq.sdk.internal.Json;
import com.transynq.sdk.models.DriverLoginRequest;
import com.transynq.sdk.models.GridQueryRequest;
import com.transynq.sdk.models.JobRepriceRequest;
import com.transynq.sdk.models.JobStatusReasonResponse;
import com.transynq.sdk.models.LongOperationResponse;
import com.transynq.sdk.models.PublicHolidayRequest;
import com.transynq.sdk.models.PublicHolidayResponse;
import com.transynq.sdk.models.ScheduleRateUpdateRequest;
import com.transynq.sdk.requests.DriverLegsQuery;
import com.transynq.sdk.requests.ImportZonesInBackgroundForm;
import com.transynq.sdk.requests.ListJobStatusReasonsQuery;
import com.transynq.sdk.requests.ListPublicHolidaysQuery;
import com.transynq.sdk.requests.UploadJobDocumentForm;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CoreTest {
    private static final UUID ID = UUID.fromString("6f1c7a52-3d6b-4b5e-9a3e-0c1d2e3f4a5b");
    private static final String HOLIDAY = "{\"id\":\"" + ID + "\",\"name\":\"Anzac Day\",\"date\":\"2026-04-25\",\"countryId\":1,\"states\":[]}";

    private MockApi api;
    private final List<Duration> delays = new ArrayList<>();

    @BeforeEach
    void start() throws Exception {
        api = new MockApi();
    }

    @AfterEach
    void stop() {
        api.close();
    }

    private TransynqClient client(Consumer<TransynqClientOptions.Builder> configure) {
        TransynqClientOptions.Builder b = TransynqClientOptions.builder(api.baseUrl())
                .sleeper(d -> {
                    synchronized (delays) {
                        delays.add(d);
                    }
                })
                .jitter(() -> 1.0);
        configure.accept(b);
        return new TransynqClient(b.build());
    }

    private TransynqClient client() {
        return client(b -> { });
    }

    private static Answer login(String session, String csrf, String expires) {
        return Answer.json(200, "{\"userName\":\"ann\",\"csrfToken\":\"" + csrf + "\",\"sessionExpiresUtc\":\"" + expires + "\"}",
                "Set-Cookie", "__Host-tq_session=" + session + "; path=/; secure; httponly");
    }

    private static JobRepriceRequest reprice() {
        return JobRepriceRequest.builder().grid(GridQueryRequest.builder().build()).dryRun(true).build();
    }

    // ------------------------------------------------------------------ contract

    @Test
    void getByIdBuildsPathHeadersAndReadsEtag() {
        api.enqueue(Answer.json(200, HOLIDAY, "ETag", "\"v1\"", "X-Api-Version", "1.0.0", "X-Correlation-Id", "abc12345"));
        ApiResponse<PublicHolidayResponse> r = client(b -> b.userAgentSuffix("acme/1.0")).referenceData().getPublicHoliday(ID);

        MockApi.Recorded req = api.requests.get(0);
        assertEquals("GET", req.method());
        assertEquals("/api/v1/public-holidays/" + ID, req.path());
        assertEquals("transynq-sdk-java/" + SdkInfo.VERSION + " acme/1.0", req.header("User-Agent"));
        assertTrue(req.header("X-Correlation-Id").matches("[0-9a-f]{32}"));
        assertEquals(LocalDate.of(2026, 4, 25), r.data().date());
        assertEquals("\"v1\"", r.etag());
        assertEquals("1.0.0", r.apiVersion());
        assertEquals("abc12345", r.correlationId());
    }

    @Test
    void queryParametersAreFormattedAndNullsSkipped() {
        api.enqueue(Answer.json(200, "{\"items\":[],\"page\":2,\"pageSize\":20,\"total\":0}"));
        client().referenceData().listPublicHolidays(ListPublicHolidaysQuery.builder().countryId(1).year(2026).page(2).pageSize(20).sort("-date,name").q("a b").build());
        assertEquals("countryId=1&year=2026&page=2&pageSize=20&sort=-date%2Cname&q=a%20b", api.requests.get(0).query());
    }

    @Test
    void requiredQueryParametersAreChecked() {
        assertThrows(NullPointerException.class, () -> ListPublicHolidaysQuery.builder().year(2026).build());
    }

    @Test
    void instantsInQueriesAreUtc() {
        api.enqueue(Answer.json(200, "[]"));
        client().driverApp().driverLegs(DriverLegsQuery.builder().from(OffsetDateTime.of(2026, 10, 2, 13, 4, 5, 0, ZoneOffset.ofHours(10))).build());
        assertEquals("from=2026-10-02T03%3A04%3A05Z", api.requests.get(0).query());
    }

    @Test
    void putSendsJsonBodyAndIfMatch() {
        api.enqueue(Answer.json(200, HOLIDAY, "ETag", "\"v2\""));
        PublicHolidayRequest body = PublicHolidayRequest.builder().id(ID).name("X").date(LocalDate.of(2026, 1, 26)).countryId(1).states(List.of("NSW")).build();
        ApiResponse<PublicHolidayResponse> r = client().referenceData().updatePublicHoliday(ID, body, RequestOptions.ifMatch("\"v1\""));

        MockApi.Recorded req = api.requests.get(0);
        assertEquals("PUT", req.method());
        assertEquals("\"v1\"", req.header("If-Match"));
        assertTrue(req.header("Content-Type").startsWith("application/json"));
        assertEquals("{\"id\":\"" + ID + "\",\"name\":\"X\",\"date\":\"2026-01-26\",\"countryId\":1,\"states\":[\"NSW\"]}", req.bodyText());
        assertEquals("\"v2\"", r.etag());
    }

    @Test
    void multipartUploadSendsTheFile() {
        api.enqueue(Answer.json(200, "{\"id\":\"" + ID + "\"}"));
        ApiResponse<ResponseBody> r = client().jobs().uploadJobDocument(ID,
                UploadJobDocumentForm.builder().file(FileUpload.of("%PDF-1.7".getBytes(), "pod.pdf", "application/pdf")).build(), null);

        MockApi.Recorded req = api.requests.get(0);
        assertTrue(req.header("Content-Type").startsWith("multipart/form-data"));
        assertTrue(req.bodyText().contains("name=\"file\"; filename=\"pod.pdf\""));
        assertTrue(req.bodyText().contains("%PDF-1.7"));
        assertEquals(ID.toString(), r.data().json().get("id").asText());
    }

    @Test
    void downloadStreamsTheFile() {
        api.enqueue(new Answer(200, "abc", Map.of("Content-Type", "application/pdf", "Content-Disposition", "attachment; filename=\"pod.pdf\"")));
        try (FileDownload file = client().jobs().downloadJobDocument(ID).data()) {
            assertEquals("application/pdf", file.contentType());
            assertEquals("pod.pdf", file.fileName());
            assertEquals("abc", new String(file.readAllBytes()));
        }
    }

    @Test
    void generatedSurfaceCoversTheSpec() throws Exception {
        assertEquals(705, SpecInfo.OPERATION_COUNT);
        assertEquals(64, SpecInfo.SHA256.length());
        TransynqClient c = client();
        int services = 0;
        for (var m : TransynqServices.class.getDeclaredMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType().getPackageName().equals("com.transynq.sdk.services")) {
                assertTrue(m.invoke(c) != null);
                services++;
            }
        }
        assertEquals(52, services);
    }

    // ------------------------------------------------------------------ auth

    @Test
    void apiKeyHeaderAndNoCsrf() {
        api.fallback = r -> Answer.json(200, "{}");
        TransynqClient c = client(b -> b.credential(new ApiKeyCredential("secret-key")));
        c.authentication().getApiClient();
        c.referenceData().deletePublicHoliday(ID, RequestOptions.ifMatch("\"x\""));
        for (MockApi.Recorded r : api.requests) {
            assertEquals("secret-key", r.header("X-Transynq-APIKey"));
            assertNull(r.header("X-CSRF-Token"));
        }
        assertFalse(new ApiKeyCredential("secret-key").toString().contains("secret-key"));
    }

    @Test
    void sessionSignsInOnceThenSendsCookieAndCsrfOnUnsafeVerbs() {
        api.enqueue(login("s1", "csrf-1", "2099-01-01T00:00:00Z")).enqueue(Answer.json(200, "{}")).enqueue(Answer.json(200, "{}"));
        SessionCredential credential = new SessionCredential("ann", "pw");
        TransynqClient c = client(b -> b.credential(credential));
        c.authentication().getMe();
        c.referenceData().deletePublicHoliday(ID, RequestOptions.ifMatch("\"x\""));

        assertEquals("/api/v1/auth/login", api.requests.get(0).path());
        assertTrue(api.requests.get(0).bodyText().contains("\"userName\":\"ann\""));
        assertTrue(api.requests.get(0).header("X-CSRF-Token") != null);
        assertEquals("__Host-tq_session=s1", api.requests.get(1).header("Cookie"));
        assertNull(api.requests.get(1).header("X-CSRF-Token"));
        assertEquals("csrf-1", api.requests.get(2).header("X-CSRF-Token"));
        assertEquals(1, credential.signInCount());
        assertEquals("ann", credential.me().get("userName").asText());
    }

    @Test
    void sessionSignsInAgainOnceAfter401() {
        api.enqueue(login("s1", "c1", "2099-01-01T00:00:00Z")).enqueue(Answer.empty(401)).enqueue(login("s2", "c2", "2099-01-01T00:00:00Z"))
                .enqueue(Answer.json(200, "{}"));
        SessionCredential credential = new SessionCredential("ann", "pw");
        client(b -> b.credential(credential)).authentication().getMe();
        assertEquals("__Host-tq_session=s2", api.requests.get(3).header("Cookie"));
        assertEquals(2, credential.signInCount());
    }

    @Test
    void failedLoginIsRaisedWithoutRetry() {
        api.enqueue(Answer.json(401, "{\"title\":\"Invalid user name or password.\"}"));
        AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> client(b -> b.credential(new SessionCredential("ann", "bad"))).authentication().getMe());
        assertEquals("Invalid user name or password.", e.title());
        assertEquals(1, api.requests.size());
    }

    @Test
    void concurrentRequestsShareOneSignIn() {
        api.fallback = r -> r.path().equals("/api/v1/auth/login") ? login("s", "c", "2099-01-01T00:00:00Z") : Answer.json(200, "{}");
        SessionCredential credential = new SessionCredential("ann", "pw");
        TransynqClient c = client(b -> b.credential(credential));
        List<CompletableFuture<ApiResponse<ResponseBody>>> calls = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            calls.add(c.authentication().getMeAsync(null));
        }
        calls.forEach(CompletableFuture::join);
        assertEquals(1, credential.signInCount());
        assertEquals(1, api.requests.stream().filter(r -> r.path().equals("/api/v1/auth/login")).count());
    }

    @Test
    void sessionIsRenewedBeforeItExpires() {
        api.fallback = r -> r.path().equals("/api/v1/auth/login") ? login("s", "c", "2026-11-16T01:00:00Z") : Answer.json(200, "{}");
        SessionCredential credential = new SessionCredential("ann", "pw");
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-16T00:00:00Z"));
        credential.clock(now::get);
        TransynqClient c = client(b -> b.credential(credential));
        c.authentication().getMe();
        now.set(now.get().plusSeconds(59 * 60 + 30));
        c.authentication().getMe();
        assertEquals(2, credential.signInCount());
    }

    @Test
    void driverLoginUsesTheBearerToken() {
        api.enqueue(Answer.json(200, "{\"token\":\"0b7c9f8e-1111-2222-3333-444455556666\",\"expiresUtc\":\"2099-01-01T00:00:00Z\",\"driver\":{},\"company\":{},\"settings\":{}}"))
                .enqueue(Answer.empty(204));
        DriverLoginRequest login = DriverLoginRequest.builder().companyCode("ACME").pin("1234").deviceId("d").pushToken("p").appVersion("1").platform("android").build();
        client(b -> b.credential(new DriverLoginCredential(login))).driverApp().driverLogout();
        assertNull(api.requests.get(0).header("Authorization"));
        assertEquals("Bearer 0b7c9f8e-1111-2222-3333-444455556666", api.requests.get(1).header("Authorization"));
    }

    @Test
    void hooksNeverSeeSecrets() {
        api.enqueue(Answer.json(200, "{}"));
        List<TransynqClientOptions.RequestInfo> seen = new ArrayList<>();
        client(b -> b.credential(new ApiKeyCredential("secret-key")).onRequest(seen::add)).authentication().getApiClient();
        assertEquals("[redacted]", seen.get(0).headers().get("X-Transynq-APIKey"));
    }

    // ------------------------------------------------------------------ retries

    @Test
    void getIsRetriedWithBackOffAndOneCorrelationId() {
        api.enqueue(Answer.empty(503)).enqueue(Answer.empty(502)).enqueue(Answer.json(200, "[]"));
        client().referenceData().listCountries();
        assertEquals(3, api.requests.size());
        assertEquals(List.of(Duration.ofMillis(500), Duration.ofMillis(1000)), delays);
        assertEquals(1, new HashSet<>(api.requests.stream().map(r -> r.header("X-Correlation-Id")).toList()).size());
    }

    @Test
    void retriesStopAtMaxAndServerErrorIsTyped() {
        api.fallback = r -> Answer.json(500, "{\"title\":\"An unexpected error occurred.\",\"code\":\"unexpected\",\"correlationId\":\"c-1\"}");
        ServerException e = assertThrows(ServerException.class, () -> client(b -> b.maxRetries(1)).referenceData().listCountries());
        assertEquals(2, api.requests.size());
        assertEquals("unexpected", e.code());
        assertEquals("c-1", e.correlationId());
    }

    @Test
    void postIsOnlyRetriedWithAnIdempotencyKey() {
        api.fallback = r -> Answer.empty(503);
        TransynqClient c = client();
        assertThrows(ServerException.class, () -> c.jobs().repriceJobs(reprice()));
        assertEquals(1, api.requests.size());

        api.fallback = null;
        api.requests.clear();
        api.enqueue(Answer.empty(503)).enqueue(Answer.json(200, "{}"));
        c.jobs().repriceJobs(reprice(), RequestOptions.idempotencyKey("key-1"));
        assertEquals(2, api.requests.size());
        api.requests.forEach(r -> assertEquals("key-1", r.header("Idempotency-Key")));
    }

    @Test
    void createWithClientIdIsRetried() {
        api.enqueue(Answer.empty(503)).enqueue(Answer.json(200, "{}"));
        client().referenceData().createPublicHoliday(PublicHolidayRequest.builder().id(Ids.newId()).name("X").build());
        assertEquals(2, api.requests.size());
    }

    @Test
    void rateLimitHonoursRetryAfterAndLongWaitsAreRaised() {
        api.enqueue(Answer.empty(429, "Retry-After", "3")).enqueue(Answer.json(200, "{}"));
        TransynqClient c = client();
        c.jobs().repriceJobs(reprice());
        assertEquals(List.of(Duration.ofSeconds(3)), delays);

        api.enqueue(Answer.empty(429, "Retry-After", "600"));
        RateLimitException e = assertThrows(RateLimitException.class, () -> c.referenceData().listCountries());
        assertEquals(Duration.ofSeconds(600), e.retryAfter());
    }

    @Test
    void networkErrorsAreRetriedThenRaised() {
        api.close();
        TransynqClient c = client(b -> b.maxRetries(2).timeout(Duration.ofSeconds(2)));
        assertThrows(TransynqConnectionException.class, () -> c.referenceData().listCountries());
        assertEquals(2, delays.size());
    }

    @Test
    void timeoutsAreRaisedAsTimeoutErrors() {
        api.fallback = r -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Answer.json(200, "[]");
        };
        assertThrows(TransynqTimeoutException.class, () -> client(b -> b.maxRetries(0).timeout(Duration.ofMillis(200))).referenceData().listCountries());
    }

    @Test
    void maxRetriesZeroAnd501AreNotRetried() {
        api.fallback = r -> Answer.empty(503);
        assertThrows(ServerException.class, () -> client(b -> b.maxRetries(0)).referenceData().listCountries());
        assertEquals(1, api.requests.size());
        api.fallback = r -> Answer.json(501, "{\"code\":\"not-implemented\"}");
        assertThrows(ServerException.class, () -> client().referenceData().listCountries());
        assertEquals(2, api.requests.size());
    }

    // ------------------------------------------------------------------ errors

    @Test
    void validationProblemExposesFieldErrors() {
        api.enqueue(Answer.json(400, "{\"title\":\"The request is not valid.\",\"status\":400,\"code\":\"validation\",\"correlationId\":\"c-9\",\"errors\":{\"name\":[\"Name is required.\"]}}"));
        ValidationException e = assertThrows(ValidationException.class, () -> client().referenceData().listCountries());
        assertEquals(400, e.status());
        assertEquals(List.of("Name is required."), e.errors().get("name"));
        assertTrue(e.rawBody().contains("\"validation\""));
    }

    @ParameterizedTest
    @CsvSource({"403,PermissionDeniedException", "404,NotFoundException", "409,ConflictException", "412,StaleVersionException",
            "422,BusinessRuleException", "418,ApiException"})
    void statusMapsToATypedError(int status, String type) {
        api.enqueue(Answer.json(status, "{\"title\":\"t\",\"detail\":\"d\",\"code\":\"year-exists\"}", "X-Correlation-Id", "hdr-1"));
        ApiException e = assertThrows(ApiException.class, () -> client().referenceData().listCountries());
        assertEquals(type, e.getClass().getSimpleName());
        assertEquals("year-exists", e.code());
        assertEquals("d", e.detail());
        assertEquals("hdr-1", e.correlationId());
        assertEquals("ListCountries", e.operationId());
        assertSame(e.getClass(), switch (status) {
            case 403 -> PermissionDeniedException.class;
            case 404 -> NotFoundException.class;
            case 409 -> ConflictException.class;
            case 412 -> StaleVersionException.class;
            case 422 -> BusinessRuleException.class;
            default -> ApiException.class;
        });
    }

    // ------------------------------------------------------------------ etag, paging, LRO

    @Test
    void readModifyWriteRetriesOnStaleVersion() {
        api.enqueue(Answer.json(200, HOLIDAY, "ETag", "\"1\"")).enqueue(Answer.json(412, "{\"code\":\"stale-version\"}"))
                .enqueue(Answer.json(200, HOLIDAY, "ETag", "\"2\"")).enqueue(Answer.json(200, HOLIDAY, "ETag", "\"3\""));
        TransynqClient c = client();
        ApiResponse<PublicHolidayResponse> saved = Concurrency.readModifyWrite(
                () -> c.referenceData().getPublicHoliday(ID),
                (current, options) -> c.referenceData().updatePublicHoliday(ID, PublicHolidayRequest.builder().id(current.id()).name(current.name() + "!")
                        .date(current.date()).countryId(current.countryId()).states(current.states()).build(), options),
                3);
        assertEquals("\"3\"", saved.etag());
        assertEquals("\"1\"", api.requests.get(1).header("If-Match"));
        assertEquals("\"2\"", api.requests.get(3).header("If-Match"));
    }

    private static String reason() {
        return "{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"Late\",\"reasonType\":\"delay\",\"active\":true}";
    }

    @Test
    void autoPagingReadsUntilTotal() {
        api.enqueue(Answer.json(200, "{\"items\":[" + reason() + "," + reason() + "],\"page\":1,\"pageSize\":2,\"total\":3}"))
                .enqueue(Answer.json(200, "{\"items\":[" + reason() + "],\"page\":2,\"pageSize\":2,\"total\":3}"));
        List<JobStatusReasonResponse> items = Paging.stream(client().referenceData()
                .listJobStatusReasonsAutoPaging(ListJobStatusReasonsQuery.builder().pageSize(2).build(), null)).toList();
        assertEquals(3, items.size());
        assertTrue(api.requests.get(1).query().contains("page=2"));
    }

    @Test
    void gridRowsIterateAcrossPages() {
        api.enqueue(Answer.json(200, "{\"rows\":[{\"a\":1},{\"a\":2}],\"page\":1,\"pageSize\":2,\"total\":3,\"summaries\":{}}"))
                .enqueue(Answer.json(200, "{\"rows\":[{\"a\":3}],\"page\":2,\"pageSize\":2,\"total\":3,\"summaries\":{}}"));
        List<Integer> rows = new ArrayList<>();
        for (ObjectNode row : client().gridRows("jobs", GridQueryRequest.builder().columns(List.of("a")).pageSize(2).build(), null)) {
            rows.add(row.get("a").asInt());
        }
        assertEquals(List.of(1, 2, 3), rows);
        assertTrue(api.requests.get(1).bodyText().contains("\"page\":2"));
    }

    private static String op(String status, String result, String error) {
        return "{\"id\":\"" + ID + "\",\"kind\":\"zones\",\"inputName\":\"z.xlsx\",\"requestedBy\":\"ann\",\"requestedUtc\":\"2026-11-16T00:00:00Z\",\"status\":\""
                + status + "\",\"startedUtc\":null,\"finishedUtc\":null,\"result\":" + result + ",\"error\":" + (error == null ? "null" : "\"" + error + "\"") + "}";
    }

    @Test
    void acceptedLongOperationIsPolledToSuccess() {
        api.enqueue(Answer.json(202, "{\"id\":\"" + ID + "\",\"status\":\"Queued\"}", "Location", "/api/v1/long-operations/" + ID))
                .enqueue(Answer.json(200, op("Running", "null", null))).enqueue(Answer.json(200, op("Succeeded", "{\"created\":3}", null)));
        TransynqClient c = client();
        ApiResponse<ResponseBody> accepted = c.imports().importZonesInBackground(ImportZonesInBackgroundForm.builder().file(FileUpload.of(new byte[] {1}, "z.xlsx", null)).build());
        assertTrue(accepted.isAccepted());
        List<String> seen = new ArrayList<>();
        LongOperationResponse done = c.pollLongOperation(accepted).waitForCompletion(Duration.ofSeconds(1), Duration.ofMinutes(1), s -> seen.add(s.status()));
        assertEquals(List.of("Running", "Succeeded"), seen);
        assertEquals(3, done.result().get("created").asInt());
        assertEquals("/api/v1/long-operations/" + ID, api.requests.get(1).path());
        assertEquals(List.of(Duration.ofSeconds(1)), delays);
    }

    @Test
    void failedLongOperationRaises() {
        api.enqueue(Answer.json(200, op("Failed", "null", "Bad file")));
        LongOperationFailedException e = assertThrows(LongOperationFailedException.class, () -> client().pollLongOperation(ID).waitForCompletion());
        assertEquals("Bad file", e.operation().error());
    }

    // ------------------------------------------------------------------ serialisation

    @Test
    void decimalsAreLosslessAndNumbersMayBeStrings() throws Exception {
        String raw = "{\"ratePercentage\":1234567890123456.78,\"minPercentage\":\"0.10\",\"decimalPlaces\":\"2\",\"unknownFutureField\":{\"x\":1}}";
        ScheduleRateUpdateRequest item = Json.mapper().readValue(raw, ScheduleRateUpdateRequest.class);
        assertEquals(new BigDecimal("1234567890123456.78"), item.ratePercentage());
        assertEquals(new BigDecimal("0.10"), item.minPercentage());
        assertEquals(2, item.decimalPlaces());
        assertTrue(Json.mapper().writeValueAsString(item).contains("\"ratePercentage\":1234567890123456.78"));
    }

    @Test
    void instantsAreUtcAndUnknownCodesAreKept() throws Exception {
        LongOperationResponse op = Json.mapper().readValue("{\"id\":\"" + ID + "\",\"kind\":\"new-kind\",\"requestedBy\":\"a\",\"requestedUtc\":\"2026-10-02T03:04:05\",\"status\":\"Paused\",\"newField\":[1]}",
                LongOperationResponse.class);
        assertEquals(OffsetDateTime.of(2026, 10, 2, 3, 4, 5, 0, ZoneOffset.UTC), op.requestedUtc());
        assertEquals("Paused", op.status());
        assertTrue(Json.mapper().writeValueAsString(op).contains("\"requestedUtc\":\"2026-10-02T03:04:05Z\""));
    }

    @Test
    void nullsAreOmittedAndDatesAreCalendarDays() throws Exception {
        String json = Json.mapper().writeValueAsString(PublicHolidayRequest.builder().id(new UUID(0, 0)).date(LocalDate.of(2026, 12, 25)).build());
        assertEquals("{\"id\":\"00000000-0000-0000-0000-000000000000\",\"date\":\"2026-12-25\"}", json);
        assertInstanceOf(PublicHolidayRequest.class, PublicHolidayRequest.builder().build().toBuilder().build());
    }
}
