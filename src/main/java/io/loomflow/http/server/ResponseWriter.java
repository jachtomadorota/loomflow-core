package io.loomflow.http.server;

import com.sun.net.httpserver.HttpExchange;
import io.loomflow.http.Response;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes a LoomFlow Response back to the JDK HttpExchange.
 */
public final class ResponseWriter {

    private ResponseWriter() {}

    public static void write(HttpExchange exchange, Response response) throws IOException {
        // Ensure Content-Type is always set
        response.headers().putIfAbsent("Content-Type", "application/json; charset=utf-8");
        response.headers().forEach((k, v) ->
                exchange.getResponseHeaders().set(k, v));

        byte[] body = response.body() != null
                ? response.body().getBytes(java.nio.charset.StandardCharsets.UTF_8)
                : new byte[0];

        exchange.sendResponseHeaders(response.statusCode(), body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    public static void writeError(HttpExchange exchange, Exception e) {
        try {
            String message = e.getMessage() != null ? e.getMessage() : "Internal Server Error";
            String json = "{\"error\":\"" + message.replace("\"", "'") + "\"}";
            byte[] body = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(500, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        } catch (IOException ignored) {}
    }
}
