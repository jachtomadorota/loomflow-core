package io.loomflow.core;

import io.loomflow.di.ServiceRegistry;
import io.loomflow.http.HttpServerAdapter;

import java.util.List;

public final class LoomFlowRuntime {
    private final ServiceRegistry registry;
    private final List<RouteDef> routes;
    private final String healthPath;

    public LoomFlowRuntime(ServiceRegistry registry, List<RouteDef> routes, String healthPath) {
        this.registry = registry;
        this.routes = routes;
        this.healthPath = healthPath;
    }

    public void start() {
        HttpServerAdapter server = new HttpServerAdapter(registry, routes, healthPath);
        server.start();
    }
}
