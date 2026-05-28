package io.loomflow.http.multipart;

/**
 * Represents a single uploaded file from a multipart/form-data request.
 */
public record UploadedFile(
        String fieldName,
        String filename,
        String contentType,
        byte[] bytes
) {
    /** File size in bytes. */
    public long size() { return bytes.length; }

    /** Content as UTF-8 string (useful for small text files). */
    public String asString() {
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
