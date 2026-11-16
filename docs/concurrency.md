# Concurrency (ETag / If-Match)

The API uses optimistic concurrency. A GET, POST or PUT answer carries an `ETag`; a PUT or DELETE must send it back in
`If-Match`:

- no `If-Match` -> **400** (`validation`);
- an `If-Match` that no longer matches -> **412** (`stale-version`): someone saved first; reload and apply your change again.

```java
ApiResponse<PublicHolidayResponse> current = client.referenceData().getPublicHoliday(id);   // ETag: "..."
client.referenceData().updatePublicHoliday(id,
        PublicHolidayRequest.builder().id(id).name("Australia Day").date(current.data().date())
                .countryId(current.data().countryId()).states(current.data().states()).build(),
        RequestOptions.ifMatch(current.etag()));                // 412 StaleVersionException if someone saved first
```

## Read-modify-write helper

The helper loads the record (with its ETag), lets you build the change, saves it with `If-Match`, and on a stale version
loads again and repeats (3 attempts by default):

```java
ApiResponse<PublicHolidayResponse> saved = Concurrency.readModifyWrite(
        () -> client.referenceData().getPublicHoliday(id),
        (current, options) -> client.referenceData().updatePublicHoliday(id, PublicHolidayRequest.builder()
                .id(current.id()).name(current.name() + " (observed)").date(current.date())
                .countryId(current.countryId()).states(current.states()).build(), options),
        3);
```

The ETag is passed through verbatim (quotes included); never build one yourself.
