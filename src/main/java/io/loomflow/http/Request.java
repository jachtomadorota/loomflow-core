package io.loomflow.http;

import io.loomflow.http.converter.ConverterRegistry;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable HTTP request representation.
 * Path params are injected by the Router after route matching.
 */
public final class Request {

    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final byte[] bodyBytes;

    // Injected by Router after matching
    private Map<String, String> pathParams = Collections.emptyMap();

    // Injected by framework at dispatch time
    private ConverterRegistry converterRegistry;

    public Request(String method, String path,
                   Map<String, String> headers,
                   Map<String, String> queryParams,
                   byte[] bodyBytes) {
        this.method      = method;
        this.path        = path;
        this.headers     = headers     != null ? headers     : Collections.emptyMap();
        this.queryParams = queryParams != null ? queryParams : Collections.emptyMap();
        this.bodyBytes   = bodyBytes   != null ? bodyBytes   : new byte[0];
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public String method() { return method; }
    public String path()   { return path; }

    /** Raw body as UTF-8 String. */
    public String body() {
        return new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Raw body bytes. */
    public byte[] bodyBytes() { return bodyBytes; }

    /** Deserialize body to given type using registered BodyConverters. */
    public <T> T body(Class<T> type) {
        if (converterRegistry == null || converterRegistry.isEmpty()) {
            throw new LoomFlowException(
                    "No BodyConverters registered. Add .converter(new JacksonBodyConverter()) to AppBuilder.");
        }
        String contentType = header("content-type");
        try {
            return converterRegistry.read(bodyBytes, type, contentType);
        } catch (IOException e) {
            throw new LoomFlowException("Failed to deserialize request body: " + e.getMessage(), e);
        }
    }

    public String header(String name) {
        return headers.get(name.toLowerCase());
    }
    public Map<String, String> headers() { return Collections.unmodifiableMap(headers); }

    public String queryParam(String name)          { return queryParams.get(name); }
    public String queryParam(String name, String d) { return queryParams.getOrDefault(name, d); }
    public Map<String, String> queryParams()       { return Collections.unmodifiableMap(queryParams); }

    // ── Path params ───────────────────────────────────────────────────────────

    /** Called by Router — not for use in application code. */
    public void setPathParams(Map<String, String> params) {
        this.pathParams = new HashMap<>(params);
    }

    public String pathParam(String name) { return pathParams.get(name); }

    public long pathLong(String name) {
        String val = pathParams.get(name);
        if (val == null) throw new IllegalArgumentException("Missing path param: " + name);
        return Long.parseLong(val);
    }

    public int pathInt(String name) {
        String val = pathParams.get(name);
        if (val == null) throw new IllegalArgumentException("Missing path param: " + name);
        return Integer.parseInt(val);
    }

    // ── Converter registry injection (package-private) ────────────────────────

    void setConverterRegistry(ConverterRegistry registry) {
        this.converterRegistry = registry;
    }

    @Override
    public String toString() { return method + " " + path; }
}
