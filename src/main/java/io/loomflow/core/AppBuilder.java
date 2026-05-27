package io.loomflow.core;

import io.loomflow.di.ServiceRegistry;
import io.loomflow.http.RouteHandler;

import java.util.ArrayList;
import java.util.List;

public final class AppBuilder {
    private final List<ServiceBinding<?, ?>> services = new ArrayList<>();
    private final List<RouteDef> routes = new ArrayList<>();
    private String healthPath = "/health";

    public <T, I extends T> AppBuilder service(Class<T> api, Class<I> impl) {
        services.add(new ServiceBinding<>(api, impl));
        return this;
    }

    public AppBuilder route(String method, String path, RouteHandler handler) {
        routes.add(new RouteDef(method, path, handler));
        return this;
    }

    public AppBuilder health(String path) {
        this.healthPath = path;
        return this;
    }

    public LoomFlowRuntime build() {
        ServiceRegistry registry = new ServiceRegistry();
        for (ServiceBinding<?, ?> binding : services) {
            registry.register(binding.api(), binding.impl());
        }
        return new LoomFlowRuntime(registry, routes, healthPath);
    }

    public void start() {
        build().start();
    }
}
