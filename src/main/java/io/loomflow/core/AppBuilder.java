package io.loomflow.core;

import io.loomflow.di.ServiceRegistry;
import io.loomflow.http.HandlerInterceptor;
import io.loomflow.http.Middleware;
import io.loomflow.http.RouteHandler;
import io.loomflow.http.converter.BodyConverter;
import io.loomflow.http.converter.ConverterRegistry;
import io.loomflow.http.cors.CorsConfig;
import io.loomflow.http.cors.CorsMiddleware;
import io.loomflow.http.staticfiles.StaticFileHandler;
import io.loomflow.security.SecurityConfig;
import io.loomflow.security.SecurityMiddleware;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent builder for a LoomFlow application.
 *
 * <pre>
 *   LoomFlow.app()
 *       .port(8080)
 *       .converter(new JacksonBodyConverter())
 *       .cors(CorsConfig.builder().allowOrigins("*").build())
 *       .security(SecurityConfig.builder()
 *           .provider(new JwtAuthenticationProvider(secret))
 *           .exclude("/health")
 *           .build())
 *       .use(Middlewares.logging())
 *       .get("/users/{id}", ctrl::getById)
 *       .group("/api/v1", g -> g
 *           .post("/users", ctrl::create)
 *           .get("/users", ctrl::list))
 *       .staticFiles("/static", java.nio.file.Paths.get("public"))
 *       .health("/health")
 *       .run();
 * </pre>
 */
public final class AppBuilder {

    private final List<ServiceBinding<?, ?>> services = new ArrayList<>();
    private final List<RouteDef> routes = new ArrayList<>();
    private final List<Middleware> middlewares = new ArrayList<>();
    private final List<HandlerInterceptor> interceptors = new ArrayList<>();
    private final ConverterRegistry converterRegistry = new ConverterRegistry();
    private String healthPath = "/health";
    private int port = 8080;
    private CorsConfig corsConfig = null;
    private SecurityConfig securityConfig = null;

    // ── Port ──────────────────────────────────────────────────────────────────

    public AppBuilder port(int port) {
        this.port = port;
        return this;
    }

    // ── Body Converters ───────────────────────────────────────────────────────

    /**
     * Registers a body converter for content negotiation and deserialization.
     * Converters are tried in registration order.
     */
    public AppBuilder converter(BodyConverter converter) {
        converterRegistry.add(converter);
        return this;
    }

    // ── CORS ──────────────────────────────────────────────────────────────────

    /**
     * Enables CORS with the given configuration.
     * CorsMiddleware is inserted as the first middleware in the pipeline.
     */
    public AppBuilder cors(CorsConfig config) {
        this.corsConfig = config;
        return this;
    }

    // ── Security ──────────────────────────────────────────────────────────────

    /**
     * Enables security with the given configuration.
     * SecurityMiddleware is inserted after CORS in the pipeline.
     */
    public AppBuilder security(SecurityConfig config) {
        this.securityConfig = config;
        return this;
    }

    // ── Services / DI ─────────────────────────────────────────────────────────

    public <T, I extends T> AppBuilder service(Class<T> api, Class<I> impl) {
        services.add(new ServiceBinding<>(api, impl));
        return this;
    }

    // ── Routes ────────────────────────────────────────────────────────────────

    public AppBuilder route(String method, String path, RouteHandler handler) {
        routes.add(new RouteDef(method, path, handler));
        return this;
    }

    public AppBuilder get(String path, RouteHandler handler) {
        return route("GET", path, handler);
    }

    public AppBuilder post(String path, RouteHandler handler) {
        return route("POST", path, handler);
    }

    public AppBuilder put(String path, RouteHandler handler) {
        return route("PUT", path, handler);
    }

    public AppBuilder patch(String path, RouteHandler handler) {
        return route("PATCH", path, handler);
    }

    public AppBuilder delete(String path, RouteHandler handler) {
        return route("DELETE", path, handler);
    }

    // ── Route Groups ──────────────────────────────────────────────────────────

    /**
     * Defines a group of routes under a shared path prefix.
     * Routes registered via the consumer are automatically prefixed.
     *
     * <pre>
     *   .group("/api/v1", g -> g
     *       .get("/users", ctrl::list)
     *       .post("/users", ctrl::create))
     * </pre>
     *
     * Implementation: uses a lightweight collector that records RouteDefs
     * into the AppBuilder's route list without requiring a live Router instance.
     */
    public AppBuilder group(String prefix, Consumer<RouteDefCollector> consumer) {
        RouteDefCollector collector = new RouteDefCollector(prefix, routes);
        consumer.accept(collector);
        return this;
    }

    // ── Interceptors ──────────────────────────────────────────────────────────

    /**
     * Adds a HandlerInterceptor applied to all route handlers.
     * Interceptors run in registration order.
     */
    public AppBuilder addInterceptor(HandlerInterceptor interceptor) {
        interceptors.add(interceptor);
        return this;
    }

    // ── Middleware ────────────────────────────────────────────────────────────

    public AppBuilder use(Middleware middleware) {
        middlewares.add(middleware);
        return this;
    }

    // ── Health ────────────────────────────────────────────────────────────────

    public AppBuilder health(String path) {
        this.healthPath = path;
        return this;
    }

    // ── Build / run ───────────────────────────────────────────────────────────

    public LoomFlowRuntime build() {
        ServiceRegistry registry = new ServiceRegistry();
        for (ServiceBinding<?, ?> binding : services) {
            registerBinding(registry, binding);
        }

        // Middleware pipeline order:
        // 1. CORS  (handles preflight before any auth)
        // 2. Security (authentication + authorization)
        // 3. User-defined middlewares
        List<Middleware> allMiddlewares = new ArrayList<>();
        if (corsConfig != null) {
            allMiddlewares.add(new CorsMiddleware(corsConfig));
        }
        if (securityConfig != null) {
            allMiddlewares.add(new SecurityMiddleware(securityConfig));
        }
        allMiddlewares.addAll(middlewares);

        return new LoomFlowRuntime(
                registry,
                routes,
                healthPath,
                port,
                allMiddlewares,
                interceptors,
                converterRegistry
        );
    }

    @SuppressWarnings("unchecked")
    private static <T, I extends T> void registerBinding(ServiceRegistry registry, ServiceBinding<T, I> binding) {
        registry.register(binding.api(), binding.impl());
    }

    /** Builds and starts the application. Blocks until shutdown. */
    public void run() {
        build().start();
    }

    /** Alias for run() — kept for backward compat. */
    public void start() {
        run();
    }

    // ── Inner class: RouteDefCollector ────────────────────────────────────────

    /**
     * Lightweight route group that collects RouteDefs into the parent list.
     * Does not require a Router instance — usable during AppBuilder assembly.
     */
    public static final class RouteDefCollector {

        private final String prefix;
        private final List<RouteDef> target;

        RouteDefCollector(String prefix, List<RouteDef> target) {
            this.prefix = prefix;
            this.target = target;
        }

        public RouteDefCollector get(String path, RouteHandler handler) {
            return add("GET", path, handler);
        }

        public RouteDefCollector post(String path, RouteHandler handler) {
            return add("POST", path, handler);
        }

        public RouteDefCollector put(String path, RouteHandler handler) {
            return add("PUT", path, handler);
        }

        public RouteDefCollector patch(String path, RouteHandler handler) {
            return add("PATCH", path, handler);
        }

        public RouteDefCollector delete(String path, RouteHandler handler) {
            return add("DELETE", path, handler);
        }

        /** Create a sub-group with a nested prefix. */
        public RouteDefCollector group(String subPrefix) {
            return new RouteDefCollector(prefix + subPrefix, target);
        }

        private RouteDefCollector add(String method, String path, RouteHandler handler) {
            target.add(new RouteDef(method, prefix + path, handler));
            return this;
        }
    }
}
