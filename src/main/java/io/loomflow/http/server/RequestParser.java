package io.loomflow.http.server;

import com.sun.net.httpserver.HttpExchange;
import io.loomflow.http.Request;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses a raw JDK HttpExchange into a LoomFlow Request.
 */
public final class RequestParser {

    private RequestParser() {}

    public static Request parse(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        String rawQuery = uri.getRawQuery();

        // Headers — normalise keys to lower-case
        Map<String, String> headers = new LinkedHashMap<>();
        exchange.getRequestHeaders().forEach((key, values) ->
                headers.put(key.toLowerCase(), String.join(", ", values)));

        // Query params
        Map<String, String> queryParams = parseQuery(rawQuery);

        // Body
        String body = new String(exchange.getRequestBody().readAllBytes());

        return new Request(method, path, headers, queryParams, body.getBytes());
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) return Collections.emptyMap();
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            String[] kv = pair.split("=", 2);
            params.put(decode(kv[0]), kv.length == 2 ? decode(kv[1]) : "");
        }
        return Collections.unmodifiableMap(params);
    }

    private static String decode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
