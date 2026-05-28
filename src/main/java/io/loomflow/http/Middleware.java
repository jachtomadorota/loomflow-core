package io.loomflow.http;

/**
 * Middleware sits in the processing chain around route handlers.
 *
 * Usage example:
 * <pre>
 *   Middleware logging = (req, next) -> {
 *       long start = System.currentTimeMillis();
 *       Response res = next.handle(req);
 *       System.out.printf("[%s %s] %d (%dms)%n",
 *           req.method(), req.path(), res.statusCode(),
 *           System.currentTimeMillis() - start);
 *       return res;
 *   };
 * </pre>
 */
@FunctionalInterface
public interface Middleware {
    Response apply(Request request, RouteHandler next) throws Exception;
}
