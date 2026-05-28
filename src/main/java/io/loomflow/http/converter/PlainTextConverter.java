package io.loomflow.http.converter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Plain text body converter for text/plain.
 * Reads to String, writes Object.toString().
 */
public final class PlainTextConverter implements BodyConverter {

    @Override
    public boolean canRead(Class<?> type, String contentType) {
        return type == String.class &&
               contentType != null && contentType.contains("text/plain");
    }

    @Override
    public boolean canWrite(Class<?> type, String acceptType) {
        return acceptType != null && acceptType.contains("text/plain");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(byte[] body, Class<T> type) throws IOException {
        return (T) new String(body, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] write(Object body) throws IOException {
        return body.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String defaultMediaType() {
        return "text/plain; charset=utf-8";
    }
}
