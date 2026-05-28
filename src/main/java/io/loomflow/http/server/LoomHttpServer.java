package io.loomflow.http.server;

import com.sun.net.httpserver.HttpServer;
import io.loomflow.http.Router;
import io.loomflow.http.Request;
import io.loomflow.http.Response;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server — zero external dependencies.
 * Uses JDK built-in com.sun.net.httpserver with a virtual thread executor (Java 21+).
 */
public final class LoomHttpServer {

    private final int port;
    private final Router router;
    private HttpServer server;

    public LoomHttpServer(int port, Router router) {
        this.port = port;
        this.router = router;
    }

    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Virtual thread per task — core LoomFlow principle
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/", exchange -> {
            try {
                Request req = RequestParser.parse(exchange);
                Response res = router.dispatch(req);
                ResponseWriter.write(exchange, res);
            } catch (Exception e) {
                ResponseWriter.writeError(exchange, e);
            }
        });

        server.start();
        System.out.printf("[LoomFlow] Server started on http://localhost:%d (virtual threads)%n", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            System.out.println("[LoomFlow] Server stopped.");
        }
    }

    public int port() {
        return port;
    }
}
