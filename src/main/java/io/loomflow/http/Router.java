package io.loomflow.http;

import io.loomflow.http.converter.ConverterRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fluent HTTP router.
 *
 * Features:
 * - path params: /users/{id}
 * - wildcard:    /files/** (captured as pathParam "**")
 * - RouteGroup:  router.group("/api/v1").get("/users", ...)
 * - global middleware chain
 * - HandlerInterceptors (pre/post/afterCompletion)
 * - pluggable 404 and error handlers
 */
public final class Router {

    private final List<RouteEntry>        routes       = new ArrayList<>();
    private final List<Middleware>        middlewares  = new ArrayList<>();
    private final List<InterceptorEntry>  interceptors = new ArrayList<>();

    private ConverterRegistry converterRegistry = new ConverterRegistry();

    /** Default constructor — uses an empty ConverterRegistry. */
    public Router() {}

    /** Constructor with pre-configured ConverterRegistry (used by AppBuilder). */
    public Router(ConverterRegistry converterRegistry) {
        this.converterRegistry = converterRegistry;
    }

    private RouteHandler notFoundHandler =
            req -> Response.notFound("No route matched: " + req.method() + " " + req.path());

    private ErrorHandler errorHandler =
            (req, ex) -> Response.internalError(
                    ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());

    // ── Route registration ─────────────────────────────────────────────────────

    public Router get(String path, RouteHandler handler)    { return register("GET",    path, handler); }
    public Router post(String path, RouteHandler handler)   { return register("POST",   path, handler); }
    public Router put(String path, RouteHandler handler)    { return register("PUT",    path, handler); }
    public Router patch(String path, RouteHandler handler)  { return register("PATCH",  path, handler); }
    public Router delete(String path, RouteHandler handler) { return register("DELETE", path, handler); }
    public Router head(String path, RouteHandler handler)   { return register("HEAD",   path, handler); }
    public Router options(String path, RouteHandler handler){ return register("OPTIONS",path, handler); }

    // ── RouteGroup ─────────────────────────────────────────────────────────────

    /** Create a route group with a common prefix. */
    public RouteGroup group(String prefix) {
        return new RouteGroup(this, prefix, Collections.emptyList());
    }

    // ── Middleware ─────────────────────────────────────────────────────────────

    public Router use(Middleware middleware) {
        middlewares.add(middleware);
        return this;
    }

    // ── Interceptors ───────────────────────────────────────────────────────────

    /** Add a global interceptor (applied to all routes). */
    public Router addInterceptor(HandlerInterceptor interceptor) {
        interceptors.add(new InterceptorEntry(interceptor, null));
        return this;
    }

    /** Add an interceptor scoped to paths matching the given Ant-style pattern. */
    public Router addInterceptor(HandlerInterceptor interceptor, String pathPattern) {
        interceptors.add(new InterceptorEntry(interceptor, antToPattern(pathPattern)));
        return this;
    }

    // ── Error / not-found ──────────────────────────────────────────────────────

    public Router onNotFound(RouteHandler handler) {
        this.notFoundHandler = handler;
        return this;
    }

    public Router onError(ErrorHandler handler) {
        this.errorHandler = handler;
        return this;
    }

    // ── ConverterRegistry ──────────────────────────────────────────────────────

    void setConverterRegistry(ConverterRegistry registry) {
        this.converterRegistry = registry;
    }

    // ── Dispatch ───────────────────────────────────────────────────────────────

    public Response dispatch(Request req) {
        // Inject converter registry so req.body(Class) works
        req.setConverterRegistry(converterRegistry);

        Response res = null;
        Exception thrown = null;
        List<HandlerInterceptor> fired = new ArrayList<>();

        try {
            for (RouteEntry entry : routes) {
                if (!entry.method().equalsIgnoreCase(req.method())) continue;

                Matcher matcher = entry.pattern().matcher(req.path());
                if (!matcher.matches()) continue;

                // Inject path params
                Map<String, String> pathParams = new HashMap<>();
                for (int i = 0; i < entry.paramNames().size(); i++) {
                    pathParams.put(entry.paramNames().get(i), matcher.group(i + 1));
                }
                req.setPathParams(pathParams);

                // Run preHandle interceptors
                for (InterceptorEntry ie : interceptors) {
                    if (ie.matches(req.path())) {
                        fired.add(ie.interceptor());
                        if (!ie.interceptor().preHandle(req)) {
                            // Interceptor aborted — return 403 or custom response
                            res = Response.forbidden("Request blocked by interceptor");
                            runPostHandle(fired, req, res, null);
                            return res;
                        }
                    }
                }

                res = executeChain(req, entry.handler());
                runPostHandle(fired, req, res, null);
                return res;
            }

            res = notFoundHandler.handle(req);
            return res;

        } catch (Exception ex) {
            thrown = ex;
            res = errorHandler.handle(req, ex);
            return res;
        } finally {
            if (res != null) {
                for (HandlerInterceptor ic : fired) {
                    ic.afterCompletion(req, res, thrown);
                }
            }
        }
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    /** Package-visible: used by RouteGroup and HttpServerAdapter. */
    Router register(String method, String path, RouteHandler handler) {
        routes.add(new RouteEntry(
                method,
                path,
                toPattern(path),
                extractParamNames(path),
                handler));
        return this;
    }

    private Response executeChain(Request req, RouteHandler finalHandler) throws Exception {
        if (middlewares.isEmpty()) return finalHandler.handle(req);
        RouteHandler chain = finalHandler;
        List<Middleware> reversed = new ArrayList<>(middlewares);
        Collections.reverse(reversed);
        for (Middleware mw : reversed) {
            RouteHandler next = chain;
            chain = r -> mw.apply(r, next);
        }
        return chain.handle(req);
    }

    private static void runPostHandle(List<HandlerInterceptor> interceptors,
                                      Request req, Response res, Exception ex) {
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            try { interceptors.get(i).postHandle(req, res); }
            catch (Exception ignored) {}
        }
    }

    /**
     * Converts path template to regex.
     * /users/{id}         → ^/users/([^/]+)$
     * /files/**           → ^/files(?:/(.*))?$
     * /users/{id:\d+}     → ^/users/(\d+)$
     */
    private static Pattern toPattern(String path) {
        if (path.endsWith("/**")) {
            String base = Pattern.quote(path.substring(0, path.length() - 3));
            return Pattern.compile("^" + base + "(?:/(.*))?$");
        }
        // Replace {name:regex} and {name}
        String regex = path.replaceAll("\\{([^/}]+):([^}]+)}", "($2)")
                           .replaceAll("\\{[^/}]+}", "([^/]+)");
        return Pattern.compile("^" + regex + "$");
    }

    /** Extracts param names: /users/{id}/orders/{oid} → ["id","oid"], /files/** → ["**"] */
    private static List<String> extractParamNames(String path) {
        if (path.endsWith("/**")) return List.of("**");
        List<String> names = new ArrayList<>();
        Matcher m = Pattern.compile("\\{([^/}:]+)(?::[^}]+)?}").matcher(path);
        while (m.find()) names.add(m.group(1));
        return Collections.unmodifiableList(names);
    }

    /** Converts Ant-style path pattern (/api/**) to a regex Pattern. */
    private static Pattern antToPattern(String pattern) {
        String regex = Pattern.quote(pattern)
                .replace("\\*\\*", "\\E.*\\Q")
                .replace("\\*",    "\\E[^/]*\\Q");
        return Pattern.compile("^" + regex + "$");
    }

    // ── Inner records ──────────────────────────────────────────────────────────

    private record RouteEntry(
            String method,
            String path,
            Pattern pattern,
            List<String> paramNames,
            RouteHandler handler) {}

    private record InterceptorEntry(HandlerInterceptor interceptor, Pattern pathPattern) {
        boolean matches(String path) {
            return pathPattern == null || pathPattern.matcher(path).matches();
        }
    }
}
