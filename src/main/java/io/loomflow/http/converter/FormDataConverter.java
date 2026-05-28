package io.loomflow.http.converter;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converter for application/x-www-form-urlencoded bodies.
 * Reads to Map<String,String>, writes Map to form-encoded string.
 */
public final class FormDataConverter implements BodyConverter {

    @Override
    public boolean canRead(Class<?> type, String contentType) {
        return contentType != null &&
               contentType.contains("application/x-www-form-urlencoded");
    }

    @Override
    public boolean canWrite(Class<?> type, String acceptType) {
        return false; // responses are JSON by default
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(byte[] body, Class<T> type) throws IOException {
        String raw = new String(body, StandardCharsets.UTF_8);
        Map<String, String> params = new LinkedHashMap<>();
        if (!raw.isBlank()) {
            for (String pair : raw.split("&")) {
                String[] kv = pair.split("=", 2);
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String val = kv.length > 1
                        ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                        : "";
                params.put(key, val);
            }
        }
        return (T) params;
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] write(Object body) throws IOException {
        Map<String, String> map = (Map<String, String>) body;
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String defaultMediaType() {
        return "application/x-www-form-urlencoded";
    }
}
