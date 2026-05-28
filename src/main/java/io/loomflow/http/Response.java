package io.loomflow.http;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mutable HTTP response builder.
 * Use static factory methods for common cases.
 */
public final class Response {

    private int statusCode;
    private String body;
    private final Map<String, String> headers = new LinkedHashMap<>();

    private Response() {}

    // ── Static factories ───────────────────────────────────────────────────────

    public static Response of(int status, String body) {
        return new Response().status(status).body(body);
    }

    public static Response ok(String body) {
        return of(200, body);
    }

    public static Response created(String body) {
        return of(201, body);
    }

    public static Response noContent() {
        return of(204, "");
    }

    public static Response badRequest(String message) {
        return of(400, json("Bad Request", message));
    }

    public static Response unauthorized(String message) {
        return of(401, json("Unauthorized", message));
    }

    public static Response forbidden(String message) {
        return of(403, json("Forbidden", message));
    }

    public static Response notFound() {
        return of(404, json("Not Found", "The requested resource was not found"));
    }

    public static Response notFound(String message) {
        return of(404, json("Not Found", message));
    }

    public static Response methodNotAllowed() {
        return of(405, json("Method Not Allowed", "HTTP method not supported for this route"));
    }

    public static Response internalError(String message) {
        return of(500, json("Internal Server Error", message));
    }

    // ── Builder methods ────────────────────────────────────────────────────────

    public Response status(int code) {
        this.statusCode = code;
        return this;
    }

    public Response body(String body) {
        this.body = body;
        return this;
    }

    public Response header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public Response contentType(String contentType) {
        return header("Content-Type", contentType);
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public int statusCode()         { return statusCode; }
    public String body()            { return body; }
    public Map<String, String> headers() { return headers; }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static String json(String error, String message) {
        return "{\"error\":\"" + escape(error) + "\",\"message\":\"" + escape(message) + "\"}";
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public String toString() {
        return "Response(" + statusCode + ")";
    }
}
