package io.loomflow.http.converter;

import java.io.IOException;

/**
 * Converts HTTP request body to a typed object and serializes response objects to bytes.
 * Register via AppBuilder.converter(new JacksonBodyConverter()).
 */
public interface BodyConverter {
    /** Whether this converter can deserialize the given type from the given Content-Type. */
    boolean canRead(Class<?> type, String contentType);

    /** Whether this converter can serialize the given object for the given Accept header. */
    boolean canWrite(Class<?> type, String acceptType);

    /** Deserialize raw bytes to the target type. */
    <T> T read(byte[] body, Class<T> type) throws IOException;

    /** Serialize object to bytes. */
    byte[] write(Object body) throws IOException;

    /** Default media type produced by this converter (used when Accept: *\/*). */
    String defaultMediaType();
}
