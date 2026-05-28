package io.loomflow.core;

import io.loomflow.di.ServiceRegistry;
import io.loomflow.http.HandlerInterceptor;
import io.loomflow.http.HttpServerAdapter;
import io.loomflow.http.Middleware;
import io.loomflow.http.converter.ConverterRegistry;

import java.util.List;

/**
 * Running instance of a LoomFlow application.
 * Created by AppBuilder.build() — do not instantiate directly.
 */
public final class LoomFlowRuntime {

    private final ServiceRegistry registry;
    private final List<RouteDef> routes;
    private final String healthPath;
    private final int port;
    private final List<Middleware> middlewares;
    private final List<HandlerInterceptor> interceptors;
    private final ConverterRegistry converterRegistry;

    private HttpServerAdapter server;

    public LoomFlowRuntime(ServiceRegistry registry,
                           List<RouteDef> routes,
                           String healthPath,
                           int port,
                           List<Middleware> middlewares,
                           List<HandlerInterceptor> interceptors,
                           ConverterRegistry converterRegistry) {
        this.registry = registry;
        this.routes = routes;
        this.healthPath = healthPath;
        this.port = port;
        this.middlewares = middlewares;
        this.interceptors = interceptors;
        this.converterRegistry = converterRegistry;
    }

    /** Backward-compat constructor (no middlewares, no interceptors, no converters). */
    public LoomFlowRuntime(ServiceRegistry registry, List<RouteDef> routes,
                           String healthPath, int port) {
        this(registry, routes, healthPath, port, List.of(), List.of(), new ConverterRegistry());
    }

    public void start() {
        server = new HttpServerAdapter(registry, routes, healthPath, port,
                middlewares, interceptors, converterRegistry);
        server.start();

        // Graceful shutdown on SIGTERM / Ctrl+C
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
            System.out.println("[LoomFlow] Shutdown signal received.");
            server.stop();
        }));
    }

    public void stop() {
        if (server != null) server.stop();
    }

    public int port() {
        return port;
    }
}
