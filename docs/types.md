# Dates, money and enums (type mapping)

| OpenAPI | Meaning | Java |
|---|---|---|
| `string` | text | `String` |
| `string` / `uuid` | identifier | `java.util.UUID` |
| `string` / `date` (C# `DateOnly`) | calendar day `yyyy-MM-dd`, never a midnight instant | `java.time.LocalDate` |
| `string` / `date-time` | instant, always UTC on the wire (`2026-10-02T03:04:05Z`) | `java.time.OffsetDateTime` (normalised to UTC; values read without an offset are UTC) |
| `integer` / `int32`, `int64` | whole number (the API also accepts it as a string) | `Integer`, `Long` |
| `number` | money, rates, quantities: `decimal(18,2)` in the API (the spec cannot tell decimals from doubles, so every number is a decimal) | `java.math.BigDecimal` (Jackson `USE_BIG_DECIMAL_FOR_FLOATS`) |
| `boolean` | flag | `Boolean` |
| `array`, `additionalProperties` | list, map | `List<T>`, `Map<String, T>` (unmodifiable) |
| untyped object, `JsonElement` | free-form JSON (grid rows, long-operation results) | `JsonNode` / `ObjectNode` |
| `binary` (`IFormFile`) | uploaded file | `FileUpload` |

Models are immutable records with builders (`PublicHolidayRequest.builder()...build()`, `record.toBuilder()`). All members are boxed types, so a nullable or absent member is `null`; `null` members are left out of request bodies (`@JsonInclude(NON_NULL)`). Unknown JSON properties are ignored. Codes such as `status` or `kind` are `String` (the contract declares no enums), so a new code never breaks deserialisation; well-known long-operation states are constants in `LongOperationStatus`.

## Rules worth knowing

- **Calendar days are not instants.** A `date` member (delivery day, public holiday) is `yyyy-MM-dd`. Never send a midnight
  instant for it.
- **Instants are UTC.** The SDK always writes them in UTC with `Z`; the company's time zone (`MeResponse.timeZone`) is for display only.
- **Money is decimal.** The API stores `decimal(18,2)` values and rounds them in its domain; the SDK never converts them through
  binary floating point.
- **Codes are open.** `status`, `kind`, `reasonType` and similar members are strings. New values may appear in later API versions;
  handle unknown values instead of failing.
- **Forward compatible.** Properties the SDK does not know yet are ignored when reading.
