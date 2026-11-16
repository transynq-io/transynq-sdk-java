# Long-running operations

Some operations (background imports, document packs, reports) queue work and answer **202 Accepted** with
`Location: /api/v1/long-operations/{id}` and a body `{ id, status }`. The operation then goes through the states
`Queued` -> `Running` -> `Succeeded` or `Failed`; `GET /api/v1/long-operations/{id}` reads it
(`{ id, kind, inputName, requestedBy, requestedUtc, status, startedUtc, finishedUtc, result, error }`), and
`GET /api/v1/long-operations/{id}/file` downloads a produced file (a workbook or a PDF).

```java
ApiResponse<ResponseBody> accepted = client.imports().importZonesInBackground(
        ImportZonesInBackgroundForm.builder().file(FileUpload.fromPath(Path.of("zones.xlsx"), null)).build());
LongOperationPoller poller = client.pollLongOperation(accepted);   // from the 202 Location header
LongOperationResponse done = poller.waitForCompletion(Duration.ofSeconds(2), Duration.ofMinutes(10),
        op -> System.out.println(op.status()));
System.out.println(done.result());                                  // JsonNode: the import report

// a report or document pack: download its file
try (FileDownload file = poller.downloadResult()) {
    file.saveTo(Path.of(file.fileName() != null ? file.fileName() : "result.xlsx"));
}
// asynchronously: poller.waitAsync(Duration.ofSeconds(2), Duration.ofMinutes(10), null).thenAccept(...)
```

`waitForCompletion` throws `LongOperationFailedException` (with `operation().error()`) when the operation ends `Failed`, and `TransynqException` after the timeout. `client.pollLongOperation(UUID)` follows an operation by id.

There is no numeric progress in the API: the progress callback receives every state read. Poll intervals of 1 to 5 seconds are
appropriate; the API also notifies the browser UI through SignalR, which SDK clients do not need.
