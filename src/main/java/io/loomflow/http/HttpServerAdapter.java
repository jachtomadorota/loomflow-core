package io.loomflow.http;

import io.loomflow.core.RouteDef;
import io.loomflow.di.ServiceRegistry;

import java.util.List;

public final class HttpServerAdapter {
    private final ServiceRegistry registry;
    private final Router router;
    private final String healthPath;

    public HttpServerAdapter(ServiceRegistry registry, List<RouteDef> routes, String healthPath) {
        this.registry = registry;
        this.router = new Router(routes);
        this.healthPath = healthPath;
    }

    public void start() {
        System.out.println("LoomFlow starting with health endpoint at " + healthPath);
        System.out.println("Registered services: " + registry.size());
        System.out.println("HTTP server adapter stub started.");
    }
}
