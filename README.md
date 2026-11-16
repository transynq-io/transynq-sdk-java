<p align="center">
  <img alt="Transynq - Intelligent Transport Operations" src="https://raw.githubusercontent.com/transynq-io/transynq-sdk-java/main/assets/brand/transynq-readme-banner.png" width="720">
</p>

<h1 align="center">Java SDK for the Transynq TMS API</h1>

<p align="center"><em>Intelligent Transport Operations</em></p>

<p align="center">
  <a href="https://github.com/transynq-io/transynq-sdk-java/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/transynq-io/transynq-sdk-java/actions/workflows/ci.yml/badge.svg?branch=main"></a>
  <a href="https://github.com/transynq-io/transynq-sdk-java/actions/workflows/release.yml"><img alt="Release" src="https://github.com/transynq-io/transynq-sdk-java/actions/workflows/release.yml/badge.svg"></a>
  <a href="https://central.sonatype.com/artifact/io.transynq/transynq-sdk"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/io.transynq/transynq-sdk?logo=apachemaven&color=2563EB"></a>
  <a href="https://javadoc.io/doc/io.transynq/transynq-sdk"><img alt="Javadoc" src="https://javadoc.io/badge2/io.transynq/transynq-sdk/javadoc.svg?color=06B6D4"></a>
  <img alt="Runtime" src="https://img.shields.io/badge/java-17%2B-ED8B00?logo=openjdk&logoColor=white">
  <a href="https://github.com/transynq-io/transynq-sdk-java/blob/main/LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/license-MIT-8B5CF6"></a>
  <a href="https://github.com/transynq-io/transynq-sdk-java/tree/main/docs/"><img alt="Docs" src="https://img.shields.io/badge/docs-guides-2563EB"></a>
  <img alt="API operations" src="https://img.shields.io/badge/API%20operations-705-0B1220">
</p>

The official Java client for **Transynq TMS** (`/api/v1`): all 705 operations of the API, grouped by area (jobs,
bookings, dispatch, invoicing, customers, driver app...), with typed models, authentication, retries, typed errors, paging,
optimistic concurrency, file transfer and long-running operations handled for you.

- Package: `io.transynq:transynq-sdk` - version 1.0.2 (semantic versioning, independent of the API version)
- Generated from the Transynq API OpenAPI 3.1 contract v1.0.0 plus a hand-written core
- Licence: [MIT](https://github.com/transynq-io/transynq-sdk-java/blob/main/LICENSE)

## Requirements

- Java 17 or later
- Jackson 2.17 (`jackson-databind`, `jackson-datatype-jsr310`), pulled in automatically

## Install

```xml
<!-- Maven -->
<dependency>
  <groupId>io.transynq</groupId>
  <artifactId>transynq-sdk</artifactId>
  <version>1.0.2</version>
</dependency>

// Gradle (Kotlin DSL)
implementation("io.transynq:transynq-sdk:1.0.2")
```

## Quick start

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

See [docs/authentication.md](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/authentication.md) for sessions, CSRF and driver tokens.

## Documentation

- [Getting started](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/getting-started.md)
- [Authentication](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/authentication.md)
- [Configuration](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/configuration.md)
- [Error handling](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/error-handling.md)
- [Pagination and grid queries](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/pagination.md)
- [Concurrency (ETag / If-Match)](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/concurrency.md)
- [Idempotency](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/idempotency.md)
- [Long-running operations](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/long-running-operations.md)
- [File upload and download](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/files.md)
- [Logging and observability](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/logging-and-observability.md)
- [Dates, money and enums (type mapping)](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/types.md)
- [Testing your integration](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/testing.md)
- [Versioning and compatibility](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/versioning.md)
- [Troubleshooting and FAQ](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/troubleshooting.md)
- [Resource index (every operation, generated)](https://github.com/transynq-io/transynq-sdk-java/blob/main/docs/resources/README.md)

Runnable samples of real flows (sign in, list and page grid rows, create a booking idempotently, update with If-Match, list
invoices, upload a POD, wait on a long operation) are in [`examples/src/main/java/io/transynq/sdk/examples/Flows.java`](https://github.com/transynq-io/transynq-sdk-java/blob/main/examples/src/main/java/io/transynq/sdk/examples/Flows.java).

The API reference is the Javadoc of the library:

```shell
./gradlew javadoc   # writes build/docs/javadoc/ (not checked in)
```

## Support

- Bugs and feature requests: [GitHub issues](https://github.com/transynq-io/transynq-sdk-java/issues) (use the templates; never paste real keys, passwords or customer data).
- Security problems: see [SECURITY.md](https://github.com/transynq-io/transynq-sdk-java/blob/main/SECURITY.md) - do not open a public issue.
- When the API answers with an error, quote its correlation id (`correlationId` on the error).

## Contributing

See [CONTRIBUTING.md](https://github.com/transynq-io/transynq-sdk-java/blob/main/CONTRIBUTING.md) and the [code of conduct](https://github.com/transynq-io/transynq-sdk-java/blob/main/CODE_OF_CONDUCT.md).

## Licence

[MIT](https://github.com/transynq-io/transynq-sdk-java/blob/main/LICENSE) - Copyright (c) 2026 Transynq.

The Transynq name and logos are trademarks of Transynq and are not covered by the MIT licence; you may use them only to refer to Transynq.
