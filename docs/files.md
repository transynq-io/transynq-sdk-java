# File upload and download

## Upload (multipart/form-data)

27 operations take files (documents, PODs, photos, imports, logos). Their parts are typed: a `FileUpload` per file part, plain
values for text parts.

```java
ApiResponse<JobPodsAddedResponse> added = client.jobs().addJobPods(jobId, AddJobPodsForm.builder()
        .files(List.of(
                FileUpload.fromPath(Path.of("pod.pdf"), "application/pdf"),
                FileUpload.of(photoBytes, "photo.jpg", "image/jpeg")))
        .build(), null);
System.out.println(added.data().ids());
```

`FileUpload.of(bytes, name, type)` and `FileUpload.fromPath(path, type)` build a part (held in memory, so a retried request can send it again).

## Download (streamed)

Operations that answer with a file (document, POD, photo and attachment downloads, exports, journals, long-operation results)
return a streamed `FileDownload` with its media type, file name (from `Content-Disposition`) and length.

```java
try (FileDownload pod = client.jobs().downloadJobPod(jobId, podId).data()) {   // streamed: close it
    System.out.println(pod.fileName() + " " + pod.contentType() + " " + pod.length());
    pod.saveTo(Path.of("pods", pod.fileName() != null ? pod.fileName() : "pod.pdf"));
}
```

## Operations with an undeclared answer

The contract does not declare the answer of about 400 operations; they return a buffered `ResponseBody` that may hold JSON, a
small file or nothing:

```java
ApiResponse<ResponseBody> answer = client.referenceData().listCountryStates(countryId);   // an operation whose answer the contract does not declare
System.out.println(answer.data().contentType() + " " + answer.data().json());
JsonNode states = answer.data().json();                                                   // or answer.data().as(SomeModel.class)
```
