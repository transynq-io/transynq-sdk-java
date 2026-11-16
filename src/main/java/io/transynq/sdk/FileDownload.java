package io.transynq.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** A streamed file answer. Close it (try-with-resources) after reading. */
public final class FileDownload implements AutoCloseable {
    private final InputStream content;
    private final String contentType;
    private final String fileName;
    private final Long length;

    /**
     * Used by the pipeline.
     *
     * @param content the stream
     * @param contentType the media type, or null
     * @param fileName the file name, or null
     * @param length the length, or null
     */
    public FileDownload(InputStream content, String contentType, String fileName, Long length) {
        this.content = content;
        this.contentType = contentType;
        this.fileName = fileName;
        this.length = length;
    }

    /** @return the content (read once) */
    public InputStream content() {
        return content;
    }

    /** @return the media type, e.g. {@code application/pdf}; or null */
    public String contentType() {
        return contentType;
    }

    /** @return the file name from {@code Content-Disposition}, or null */
    public String fileName() {
        return fileName;
    }

    /** @return the length when the server sent it, or null */
    public Long length() {
        return length;
    }

    /** @return the whole content */
    public byte[] readAllBytes() {
        try {
            return content.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Copies the content to a file (replacing it).
     *
     * @param path the destination
     */
    public void saveTo(Path path) {
        try {
            Files.copy(content, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            content.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
