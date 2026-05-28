package io.loomflow.http.staticfiles;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Serves static files from a filesystem directory.
 * Supports: ETag, Cache-Control, 304 Not Modified, path-traversal guard.
 *
 * Register via AppBuilder.staticFiles("/prefix", root).
 */
public final class StaticFileHandler implements HttpHandler {

    private static final DateTimeFormatter HTTP_DATE =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH)
                             .withZone(ZoneId.of("GMT"));

    private final String urlPrefix;
    private final Path   root;
    private final String cacheControl;

    public StaticFileHandler(String urlPrefix, Path root, String cacheControl) {
        this.urlPrefix    = urlPrefix.endsWith("/") ? urlPrefix : urlPrefix + "/";
        this.root         = root.toAbsolutePath().normalize();
        this.cacheControl = cacheControl;
    }

    public StaticFileHandler(String urlPrefix, Path root) {
        this(urlPrefix, root, "max-age=3600");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String rawPath = exchange.getRequestURI().getPath();
        String relative = rawPath.startsWith(urlPrefix)
                ? rawPath.substring(urlPrefix.length() - 1)
                : rawPath;

        Path target = root.resolve(relative.startsWith("/")
                ? relative.substring(1) : relative).normalize();

        // Path traversal guard
        if (!target.startsWith(root)) {
            send(exchange, 403, "Forbidden");
            return;
        }

        // Directory → try index.html
        if (Files.isDirectory(target)) {
            target = target.resolve("index.html");
        }

        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            send(exchange, 404, "Not Found");
            return;
        }

        // ETag based on last-modified millis
        String etag = "\"" + Files.getLastModifiedTime(target).toMillis() + "\"";
        String ifNoneMatch = exchange.getRequestHeaders().getFirst("If-None-Match");
        if (etag.equals(ifNoneMatch)) {
            exchange.sendResponseHeaders(304, -1);
            exchange.getResponseBody().close();
            return;
        }

        String contentType = probeContentType(target);
        exchange.getResponseHeaders().set("Content-Type",  contentType);
        exchange.getResponseHeaders().set("ETag",          etag);
        exchange.getResponseHeaders().set("Cache-Control", cacheControl);
        exchange.getResponseHeaders().set("Last-Modified",
                HTTP_DATE.format(Files.getLastModifiedTime(target).toInstant()));

        long size = Files.size(target);
        exchange.sendResponseHeaders(200, size);
        try (OutputStream out = exchange.getResponseBody()) {
            Files.copy(target, out);
        }
    }

    private static String probeContentType(Path path) {
        try {
            String ct = Files.probeContentType(path);
            if (ct != null) return ct;
        } catch (IOException ignored) {}
        // manual fallback
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".js"))   return "application/javascript";
        if (name.endsWith(".css"))  return "text/css";
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".json")) return "application/json";
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".svg"))  return "image/svg+xml";
        if (name.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
