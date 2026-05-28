package io.loomflow.http.converter;

import io.loomflow.http.LoomFlowException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ordered registry of BodyConverters.
 * Selects converter based on Content-Type / Accept headers.
 */
public final class ConverterRegistry {

    private final List<BodyConverter> converters = new ArrayList<>();

    public ConverterRegistry add(BodyConverter converter) {
        converters.add(converter);
        return this;
    }

    public List<BodyConverter> all() {
        return Collections.unmodifiableList(converters);
    }

    /** Deserialize request body to given type using Content-Type header. */
    public <T> T read(byte[] body, Class<T> type, String contentType) throws IOException {
        for (BodyConverter c : converters) {
            if (c.canRead(type, contentType)) {
                return c.read(body, type);
            }
        }
        throw new LoomFlowException(
                "No BodyConverter found for Content-Type '" + contentType +
                "' and type " + type.getSimpleName());
    }

    /** Serialize object to bytes using Accept header (falls back to first that canWrite). */
    public byte[] write(Object body, String acceptType) throws IOException {
        for (BodyConverter c : converters) {
            if (c.canWrite(body.getClass(), acceptType)) {
                return c.write(body);
            }
        }
        // fallback — first converter that can write anything
        for (BodyConverter c : converters) {
            if (c.canWrite(body.getClass(), "*/*")) {
                return c.write(body);
            }
        }
        throw new LoomFlowException(
                "No BodyConverter can write type " + body.getClass().getSimpleName());
    }

    /** Media type produced for the given accept header. */
    public String mediaTypeFor(Object body, String acceptType) {
        for (BodyConverter c : converters) {
            if (c.canWrite(body.getClass(), acceptType)) {
                return c.defaultMediaType();
            }
        }
        return converters.isEmpty() ? "application/octet-stream"
                                    : converters.get(0).defaultMediaType();
    }

    public boolean isEmpty() {
        return converters.isEmpty();
    }
}
