<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/qadir0108/transynq-sdk-java/main/assets/brand/transynq-logo-dark.png">
    <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/qadir0108/transynq-sdk-java/main/assets/brand/transynq-logo-light.png">
    <img alt="Transynq" src="https://raw.githubusercontent.com/qadir0108/transynq-sdk-java/main/assets/brand/transynq-logo-light.png" width="360">
  </picture>
</p>

<h1 align="center">Java SDK for the Transynq TMS API</h1>

<p align="center"><em>Intelligent Transport Operations</em></p>

The official Java client for **Transynq TMS** (`/api/v1`): all 705 operations of the API, grouped by area (jobs,
bookings, dispatch, invoicing, customers, driver app...), with typed models, authentication, retries, typed errors, paging,
optimistic concurrency, file transfer and long-running operations handled for you.

- Package: `com.transynq:transynq-sdk` - version 1.0.0 (semantic versioning, independent of the API version)
- Generated from the Transynq API OpenAPI 3.1 contract v1.0.0 plus a hand-written core
- Licence: [MIT](LICENSE)

## Requirements

- Java 17 or later
- Jackson 2.17 (`jackson-databind`, `jackson-datatype-jsr310`), pulled in automatically

## Install

```xml
<!-- Maven -->
<dependency>
  <groupId>com.transynq</groupId>
  <artifactId>transynq-sdk</artifactId>
  <version>1.0.0</version>
</dependency>

// Gradle (Kotlin DSL)
implementation("com.transynq:transynq-sdk:1.0.0")
```

## Quick start

```java
import com.transynq.sdk.ApiResponse;
import com.transynq.sdk.TransynqClient;
import com.transynq.sdk.TransynqClientOptions;
import com.transynq.sdk.credentials.ApiKeyCredential;
import com.transynq.sdk.models.CountryResponse;
import java.util.List;

TransynqClient client = new TransynqClient(TransynqClientOptions.builder("https://api.example.com")
        .credential(new ApiKeyCredential(System.getenv("TRANSYNQ_API_KEY")))
        .build());

ApiResponse<List<CountryResponse>> countries = client.referenceData().listCountries();
countries.data().forEach(c -> System.out.println(c.name()));
System.out.println("Answered by API " + countries.apiVersion() + ", correlation id " + countries.correlationId());
```

One accessor per OpenAPI tag (`client.jobs()`, `client.referenceData()`, `client.driverApp()`...), one method per operation named after its `operationId` in camel case (`getJob`, `listCountries`), each with an overload taking `RequestOptions` and a `...Async` twin returning `CompletableFuture`. Path parameters come first, then the body (or a generated `<Operation>Form`), then a generated `<Operation>Query` record (built with `builder()`).

Every call returns an `ApiResponse`: `ApiResponse<T>` exposes `data()`, `statusCode()`, `headers()`, `etag()`, `apiVersion()` (`X-Api-Version`), `correlationId()` (`X-Correlation-Id`), `location()` and `isAccepted()`.

## Authentication

The API accepts three non-browser credentials; pick the one that matches your integration.

| Credential | When | Wire |
|---|---|---|
| `ApiKeyCredential` | server-to-server integrations | `X-Transynq-APIKey: <key>` |
| `SessionCredential` | tools acting as a named user | `POST /api/v1/auth/login`, then the session cookie + `X-CSRF-Token` |
| `DriverLoginCredential` / `DriverTokenCredential` | driver / POD apps (`/api/v1/driver/*`) | `Authorization: Bearer <token>` |

```java
TransynqClient client = new TransynqClient(TransynqClientOptions.builder("https://api.example.com")
        .credential(new ApiKeyCredential("tq_example_key_123"))   // sent as X-Transynq-APIKey
        .build());
System.out.println(client.authentication().getApiClient().data().json());   // {"companyId":..., "description":...}
```

See [docs/authentication.md](docs/authentication.md) for sessions, CSRF and driver tokens.

## Documentation

- [Getting started](docs/getting-started.md)
- [Authentication](docs/authentication.md)
- [Configuration](docs/configuration.md)
- [Error handling](docs/error-handling.md)
- [Pagination and grid queries](docs/pagination.md)
- [Concurrency (ETag / If-Match)](docs/concurrency.md)
- [Idempotency](docs/idempotency.md)
- [Long-running operations](docs/long-running-operations.md)
- [File upload and download](docs/files.md)
- [Logging and observability](docs/logging-and-observability.md)
- [Dates, money and enums (type mapping)](docs/types.md)
- [Testing your integration](docs/testing.md)
- [Versioning and compatibility](docs/versioning.md)
- [Troubleshooting and FAQ](docs/troubleshooting.md)
- [Resource index (every operation, generated)](docs/resources/README.md)

Runnable samples of real flows (sign in, list and page grid rows, create a booking idempotently, update with If-Match, list
invoices, upload a POD, wait on a long operation) are in [`examples/src/main/java/com/transynq/sdk/examples/Flows.java`](examples/src/main/java/com/transynq/sdk/examples/Flows.java).

The API reference is the Javadoc of the library:

```shell
./gradlew javadoc   # writes build/docs/javadoc/ (not checked in)
```

## Support

- Bugs and feature requests: [GitHub issues](https://github.com/qadir0108/transynq-sdk-java/issues) (use the templates; never paste real keys, passwords or customer data).
- Security problems: see [SECURITY.md](SECURITY.md) - do not open a public issue.
- When the API answers with an error, quote its correlation id (`correlationId` on the error).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and the [code of conduct](CODE_OF_CONDUCT.md).

## Licence

[MIT](LICENSE) - Copyright (c) 2026 Transynq.

The Transynq name and logos are trademarks of Transynq and are not covered by the MIT licence; you may use them only to refer to Transynq.
