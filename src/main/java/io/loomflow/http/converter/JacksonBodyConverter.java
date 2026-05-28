package io.loomflow.http.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;

/**
 * JSON body converter backed by Jackson.
 * Handles application/json and application/*+json.
 */
public final class JacksonBodyConverter implements BodyConverter {

    private final ObjectMapper mapper;

    public JacksonBodyConverter() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public JacksonBodyConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean canRead(Class<?> type, String contentType) {
        return contentType != null &&
               (contentType.contains("application/json") || contentType.contains("+json"));
    }

    @Override
    public boolean canWrite(Class<?> type, String acceptType) {
        if (acceptType == null || acceptType.contains("*/*")) return true;
        return acceptType.contains("application/json") || acceptType.contains("+json");
    }

    @Override
    public <T> T read(byte[] body, Class<T> type) throws IOException {
        return mapper.readValue(body, type);
    }

    @Override
    public byte[] write(Object body) throws IOException {
        return mapper.writeValueAsBytes(body);
    }

    @Override
    public String defaultMediaType() {
        return "application/json; charset=utf-8";
    }

    /** Expose mapper for advanced use. */
    public ObjectMapper mapper() {
        return mapper;
    }
}
