package io.loomflow.http.cors;

import io.loomflow.http.Middleware;
import io.loomflow.http.Request;
import io.loomflow.http.Response;
import io.loomflow.http.RouteHandler;

import java.util.stream.Collectors;

/**
 * CORS middleware — handles preflight OPTIONS and adds Access-Control-* headers.
 * Register via AppBuilder.cors(config) or router.use(new CorsMiddleware(config)).
 */
public final class CorsMiddleware implements Middleware {

    private final CorsConfig config;

    public CorsMiddleware(CorsConfig config) {
        this.config = config;
    }

    @Override
    public Response apply(Request req, RouteHandler next) throws Exception {
        String origin = req.header("origin");

        // Preflight — short-circuit, never reaches the handler
        if ("OPTIONS".equalsIgnoreCase(req.method())
                && req.header("access-control-request-method") != null) {
            Response res = Response.noContent();
            applyCorsHeaders(res, origin);
            res.header("Access-Control-Allow-Methods",
                    String.join(", ", config.allowedMethods()));
            res.header("Access-Control-Allow-Headers",
                    String.join(", ", config.allowedHeaders()));
            res.header("Access-Control-Max-Age", String.valueOf(config.maxAge()));
            return res;
        }

        Response res = next.handle(req);
        applyCorsHeaders(res, origin);
        return res;
    }

    private void applyCorsHeaders(Response res, String origin) {
        if (origin == null) return;
        if (config.isOriginAllowed(origin)) {
            // Reflect specific origin (required when credentials=true)
            res.header("Access-Control-Allow-Origin",
                    config.allowedOrigins().contains("*") ? "*" : origin);
            res.header("Vary", "Origin");
        }
        if (config.allowCredentials()) {
            res.header("Access-Control-Allow-Credentials", "true");
        }
    }
}
