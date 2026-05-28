package io.loomflow.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Groups routes under a common path prefix with optional scoped middleware.
 *
 * <pre>
 *   RouteGroup api = router.group("/api/v1");
 *   api.get("/users",      ctrl::list);
 *   api.post("/users",     ctrl::create);
 *
 *   RouteGroup admin = api.group("/admin").use(authMiddleware);
 *   admin.delete("/users/{id}", adminCtrl::delete);
 * </pre>
 */
public final class RouteGroup {

    private final Router           router;
    private final String           prefix;
    private final List<Middleware> middlewares;

    RouteGroup(Router router, String prefix, List<Middleware> middlewares) {
        this.router      = router;
        this.prefix      = prefix;
        this.middlewares = Collections.unmodifiableList(middlewares);
    }

    /** Create a nested group with an additional prefix segment. */
    public RouteGroup group(String subPrefix) {
        return new RouteGroup(router, prefix + subPrefix, new ArrayList<>(middlewares));
    }

    /** Add a middleware scoped to this group (returns new group — immutable). */
    public RouteGroup use(Middleware middleware) {
        List<Middleware> next = new ArrayList<>(middlewares);
        next.add(middleware);
        return new RouteGroup(router, prefix, next);
    }

    public RouteGroup get(String path, RouteHandler handler) {
        return add("GET", path, handler);
    }
    public RouteGroup post(String path, RouteHandler handler) {
        return add("POST", path, handler);
    }
    public RouteGroup put(String path, RouteHandler handler) {
        return add("PUT", path, handler);
    }
    public RouteGroup patch(String path, RouteHandler handler) {
        return add("PATCH", path, handler);
    }
    public RouteGroup delete(String path, RouteHandler handler) {
        return add("DELETE", path, handler);
    }

    private RouteGroup add(String method, String path, RouteHandler handler) {
        RouteHandler wrapped = wrapWithMiddlewares(handler);
        router.register(method, prefix + path, wrapped);
        return this;
    }

    private RouteHandler wrapWithMiddlewares(RouteHandler handler) {
        if (middlewares.isEmpty()) return handler;
        RouteHandler chain = handler;
        List<Middleware> reversed = new ArrayList<>(middlewares);
        Collections.reverse(reversed);
        for (Middleware mw : reversed) {
            RouteHandler next = chain;
            chain = req -> mw.apply(req, next);
        }
        return chain;
    }
}
