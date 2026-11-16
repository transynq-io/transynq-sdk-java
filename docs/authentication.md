# Authentication

The Transynq API has no OAuth flow; it accepts exactly these credentials, and the SDK supports each of them.

## API client key

An API key belongs to one company: everything it reads and writes is that company's data. It is sent in the
`X-Transynq-APIKey` header. API-key requests carry no cookie, so they need no CSRF token.

```java
TransynqClient client = new TransynqClient(TransynqClientOptions.builder("https://api.example.com")
        .credential(new ApiKeyCredential("tq_example_key_123"))   // sent as X-Transynq-APIKey
        .build());
System.out.println(client.authentication().getApiClient().data().json());   // {"companyId":..., "description":...}
```

`GET /api/v1/api-client` tells which company the key acts for (never the key itself).

## Session (user name and password)

`SessionCredential` signs in with `POST /api/v1/auth/login` the first time a request needs it. The API answers with a session
cookie (`tq_session`, or `__Host-tq_session` outside development; HttpOnly, carrying only a session id) and the signed-in user's
profile (`MeResponse`) including a **CSRF token** bound to the session. The credential then sends:

- the session cookie on every request;
- `X-CSRF-Token: <token>` on every unsafe request (POST, PUT, PATCH, DELETE). Without it the API answers 403
  "Missing or invalid X-CSRF-Token.".

```java
SessionCredential session = new SessionCredential("dispatcher@example.com", System.getenv("TRANSYNQ_PASSWORD"));
TransynqClient client = new TransynqClient(TransynqClientOptions.builder("https://api.example.com").credential(session).build());

client.authentication().getMe();                         // signs in on first use
System.out.println(session.me().get("displayName"));     // the login answer (MeResponse)
client.signOut();                                         // POST /api/v1/auth/logout and forget the session
```

Sessions end after an idle period (60 minutes by default) and after an absolute lifetime (12 hours by default). The credential
signs in again 60 seconds before the absolute end, and once more when a request answers 401 (idle timeout, revocation, a password
change). Concurrent requests share one sign-in. A failed sign-in raises the authentication error with the API's message
(for example "Invalid user name or password." or "Your account is locked.").

The sign-in endpoint is rate limited (10 attempts per minute by default): do not create a new credential per request; create one
client and reuse it.

## Driver token

The driver app's endpoints (`/api/v1/driver/*`) use a bearer token issued by `POST /api/v1/driver/login` (company code, PIN and
device details). `DriverLoginCredential` signs in, keeps the token, renews it before `expiresUtc` and after a 401;
`DriverTokenCredential` sends a token you already have.

```java
DriverLoginCredential driver = new DriverLoginCredential(DriverLoginRequest.builder()
        .companyCode("ACME").pin("1234").deviceId("device-1").pushToken("push-1").appVersion("1.0.0").platform("android")
        .build());
DriverTokenCredential existing = new DriverTokenCredential("0b7c9f8e-1111-2222-3333-444455556666");   // a token you already have

TransynqClient client = new TransynqClient(TransynqClientOptions.builder("https://api.example.com").credential(driver).build());
List<DriverLeg> legs = client.driverApp().driverLegs(DriverLegsQuery.builder().state("open").build()).data();
```

## Not supported

- Microsoft (Entra ID) sign-in is a browser redirect flow; it is not available to SDK clients.
- Internal service keys used between Transynq back ends are not part of the public API.

## Custom credentials

Implement `Credential` (`apply` adds headers, `refreshAfterUnauthorized` decides whether a 401 is worth one retry), or extend `SignInCredential<S>` for single-flight sign-in with refresh before expiry.

## Keeping secrets safe

Credentials never appear in logs: hooks see `Authorization`, `Cookie`, `Set-Cookie`, `X-Transynq-APIKey` and `X-CSRF-Token` as
`[redacted]`, and credential objects print as `...([redacted])`. Request and response bodies are never logged by the SDK.
