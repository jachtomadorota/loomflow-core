package io.loomflow.http;

import io.loomflow.core.RouteDef;
import io.loomflow.di.ServiceRegistry;
import io.loomflow.http.converter.ConverterRegistry;
import io.loomflow.http.server.LoomHttpServer;

import java.util.List;

/**
 * Bridges LoomFlowRuntime to the embedded LoomHttpServer.
 * Builds a Router from registered RouteDefs, wires middleware/interceptors/converters,
 * then delegates to LoomHttpServer.
 */
public final class HttpServerAdapter {

    private final Router router;
    private final LoomHttpServer server;

    /** Full constructor — used by LoomFlowRuntime. */
    public HttpServerAdapter(ServiceRegistry registry,
                             List<RouteDef> routes,
                             String healthPath,
                             int port,
                             List<Middleware> middlewares,
                             List<HandlerInterceptor> interceptors,
                             ConverterRegistry converterRegistry) {
        this.router = buildRouter(routes, healthPath, middlewares, interceptors, converterRegistry);
        this.server = new LoomHttpServer(port, router);
    }

    /** Backward-compat constructor — no middlewares, no interceptors, no converters. */
    public HttpServerAdapter(ServiceRegistry registry, List<RouteDef> routes,
                             String healthPath, int port) {
        this(registry, routes, healthPath, port, List.of(), List.of(), new ConverterRegistry());
    }

    public void start() {
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException("[LoomFlow] Failed to start HTTP server", e);
        }
    }

    public void stop() {
        server.stop();
    }

    public int port() {
        return server.port();
    }

    // ── Router assembly ────────────────────────────────────────────────────────

    private static Router buildRouter(List<RouteDef> routes,
                                      String healthPath,
                                      List<Middleware> middlewares,
                                      List<HandlerInterceptor> interceptors,
                                      ConverterRegistry converterRegistry) {
        Router router = new Router(converterRegistry);

        // Register global middlewares
        for (Middleware middleware : middlewares) {
            router.use(middleware);
        }

        // Register global interceptors
        for (HandlerInterceptor interceptor : interceptors) {
            router.addInterceptor(interceptor);
        }

        // Built-in health endpoint
        router.get(healthPath, req -> Response.ok(
                "{\"status\":\"UP\",\"timestamp\":\"" + java.time.Instant.now() + "\"}"
        ));

        // Application routes
        for (RouteDef def : routes) {
            router.register(def.method(), def.path(), def.handler());
        }

        return router;
    }
}
