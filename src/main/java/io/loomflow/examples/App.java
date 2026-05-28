package io.loomflow.examples;

import io.loomflow.LoomFlow;
import io.loomflow.http.Middleware;
import io.loomflow.http.Response;
import io.loomflow.http.converter.JacksonBodyConverter;
import io.loomflow.http.cors.CorsConfig;
import io.loomflow.security.SecurityConfig;
import io.loomflow.security.SecurityContext;
import io.loomflow.security.apikey.ApiKeyAuthenticationProvider;
import io.loomflow.security.jwt.JwtAuthenticationProvider;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Example application demonstrating LoomFlow v0.3:
 * - embedded virtual-thread HTTP server
 * - body converters (Jackson JSON)
 * - CORS
 * - security (JWT + API key, ScopedValue-based SecurityContext)
 * - route groups
 * - validation
 * - logging middleware
 * - health endpoint
 */
public class App {

    // Simulated API key store (in production: database lookup)
    private static final Map<String, String> API_KEYS = new ConcurrentHashMap<>(Map.of(
            "test-api-key-123", "service-account"
    ));

    public static void main(String[] args) {

        // ── Logging middleware ─────────────────────────────────────────────────
        Middleware logging = (req, next) -> {
            long start = System.currentTimeMillis();
            Response res = next.handle(req);
            System.out.printf("[LoomFlow] %s %s → %d (%dms) user=%s%n",
                    req.method(), req.path(), res.statusCode(),
                    System.currentTimeMillis() - start,
                    SecurityContext.current().name());
            return res;
        };

        // ── Security config ────────────────────────────────────────────────────
        // JWT secret must be at least 32 chars for HS256
        String jwtSecret = "loomflow-dev-secret-key-min32chars!!";

        SecurityConfig security = SecurityConfig.builder()
                .provider(new JwtAuthenticationProvider(jwtSecret))
                .provider(ApiKeyAuthenticationProvider.builder()
                        .headerName("X-Api-Key")
                        .keyResolver(key -> {
                            String userId = API_KEYS.get(key);
                            if (userId == null) return Optional.empty();
                            return Optional.of(io.loomflow.security.Principal.builder(userId)
                                    .role("SERVICE")
                                    .build());
                        })
                        .build())
                .policy(io.loomflow.security.AuthorizationPolicy.authenticated())
                .exclude("/health", "/public/**")
                .allowAnonymous(false)
                .build();

        // ── CORS config ────────────────────────────────────────────────────────
        CorsConfig cors = CorsConfig.builder()
                .allowOrigins("http://localhost:3000", "https://app.example.com")
                .allowMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowHeaders("Content-Type", "Authorization", "X-Api-Key")
                .allowCredentials(true)
                .maxAge(3600)
                .build();

        // ── Services ───────────────────────────────────────────────────────────
        UserServiceImpl userService = new UserServiceImpl();
        UserController controller = new UserController(userService);

        // ── Application ────────────────────────────────────────────────────────
        LoomFlow.app()
                .port(8080)
                .converter(new JacksonBodyConverter())
                .cors(cors)
                .security(security)
                .use(logging)

                // Public endpoints (excluded from security)
                .get("/public/info", req -> Response.ok(
                        "{\"framework\":\"LoomFlow\",\"version\":\"0.3.0\"}"
                ))

                // Route group — /api/v1 (g is AppBuilder.RouteDefCollector)
                .group("/api/v1", g -> g
                        .get("/users/{id}", controller::getById)
                        .post("/users", controller::create)
                        .get("/users", req -> {
                            // Example: access current principal from SecurityContext
                            String currentUser = SecurityContext.current().name();
                            return Response.ok("{\"requestedBy\":\"" + currentUser + "\"}");
                        })
                        .delete("/users/{id}", req -> {
                            // Require ADMIN role explicitly
                            SecurityContext.requireRole("ADMIN");
                            return Response.noContent();
                        })
                )

                // Legacy routes (backward compat)
                .get("/users/{id}", controller::getById)
                .post("/users", controller::create)

                .health("/health")
                .run();
    }
}
