# Getting started

## 1. Install

```xml
<!-- Maven -->
<dependency>
  <groupId>io.transynq</groupId>
  <artifactId>transynq-sdk</artifactId>
  <version>1.0.0</version>
</dependency>

// Gradle (Kotlin DSL)
implementation("io.transynq:transynq-sdk:1.0.0")
```

## 2. Get a credential

Ask your Transynq administrator for an **API client key** (recommended for integrations: it is tied to one company and needs no
password), or use a user name and password for a session. Keep secrets out of source control: read them from the environment or a
secret store.

## 3. Make a first call

```java
import io.transynq.sdk.ApiResponse;
import io.transynq.sdk.TransynqClient;
import io.transynq.sdk.TransynqClientOptions;
import io.transynq.sdk.credentials.ApiKeyCredential;
import io.transynq.sdk.models.CountryResponse;
import java.util.List;

TransynqClient client = new TransynqClient(TransynqClientOptions.builder("https://api.example.com")
        .credential(new ApiKeyCredential(System.getenv("TRANSYNQ_API_KEY")))
        .build());

ApiResponse<List<CountryResponse>> countries = client.referenceData().listCountries();
countries.data().forEach(c -> System.out.println(c.name()));
System.out.println("Answered by API " + countries.apiVersion() + ", correlation id " + countries.correlationId());
```

## 4. Learn the shape of the client

One accessor per OpenAPI tag (`client.jobs()`, `client.referenceData()`, `client.driverApp()`...), one method per operation named after its `operationId` in camel case (`getJob`, `listCountries`), each with an overload taking `RequestOptions` and a `...Async` twin returning `CompletableFuture`. Path parameters come first, then the body (or a generated `<Operation>Form`), then a generated `<Operation>Query` record (built with `builder()`).

- Every answer is an `ApiResponse` with the data and the headers you need: `ApiResponse<T>` exposes `data()`, `statusCode()`, `headers()`, `etag()`, `apiVersion()` (`X-Api-Version`), `correlationId()` (`X-Correlation-Id`), `location()` and `isAccepted()`.
- About 400 operations do not declare their answer in the contract (they return JSON, a file or nothing). They return a buffered
  `ResponseBody` (bytes, content type, file name) with JSON helpers; clear file downloads return a streamed `FileDownload`.
- The [resource index](resources/README.md) lists every operation with its method name.

## 5. Next steps

- [Authentication](authentication.md) - sessions, CSRF and driver tokens.
- [Error handling](error-handling.md) - typed errors and correlation ids.
- [Pagination](pagination.md), [Concurrency](concurrency.md), [Idempotency](idempotency.md), [Long-running operations](long-running-operations.md).
- The runnable samples in `examples/src/main/java/io/transynq/sdk/examples/Flows.java` (`./gradlew runExample --args=sign-in`).
