package io.loomflow.http;

import io.loomflow.core.RouteDef;

import java.util.List;
import java.util.Map;

public final class Router {
    private final List<RouteDef> routes;

    public Router(List<RouteDef> routes) {
        this.routes = routes;
    }

    public Response route(Request request) {
        for (RouteDef route : routes) {
            if (route.method().equalsIgnoreCase(request.method()) && matches(route.path(), request.path())) {
                return route.handler().handle(request);
            }
        }
        return Response.notFound();
    }

    private boolean matches(String template, String path) {
        String[] t = template.split("/");
        String[] p = path.split("/");
        if (t.length != p.length) return false;
        for (int i = 0; i < t.length; i++) {
            if (t[i].startsWith("{") && t[i].endsWith("}")) continue;
            if (!t[i].equals(p[i])) return false;
        }
        return true;
    }
}
