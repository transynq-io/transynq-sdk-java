package com.transynq.sdk;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** A file to upload in a multipart form (held in memory, so a retried request can send it again). */
public final class FileUpload {
    private final byte[] content;
    private final String fileName;
    private final String contentType;

    private FileUpload(byte[] content, String fileName, String contentType) {
        this.content = Objects.requireNonNull(content, "content");
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.contentType = contentType == null ? "application/octet-stream" : contentType;
    }

    /**
     * A file from bytes.
     *
     * @param content the bytes
     * @param fileName the file name
     * @param contentType the media type (null for {@code application/octet-stream})
     * @return the upload
     */
    public static FileUpload of(byte[] content, String fileName, String contentType) {
        return new FileUpload(content.clone(), fileName, contentType);
    }

    /**
     * A file read from disk.
     *
     * @param path the path
     * @param contentType the media type (null for {@code application/octet-stream})
     * @return the upload
     */
    public static FileUpload fromPath(Path path, String contentType) {
        try {
            return new FileUpload(Files.readAllBytes(path), path.getFileName().toString(), contentType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** @return the bytes */
    public byte[] content() {
        return content.clone();
    }

    /** @return the file name sent to the API */
    public String fileName() {
        return fileName;
    }

    /** @return the media type sent to the API */
    public String contentType() {
        return contentType;
    }
}
