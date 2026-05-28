package io.loomflow.http;

/**
 * Intercepts request handling at three points:
 * - preHandle: before the handler runs (return false to abort)
 * - postHandle: after the handler returns a response
 * - afterCompletion: always, even if an exception occurred
 */
public interface HandlerInterceptor {

    /**
     * Called before the route handler.
     * Return false to abort further processing — you must set a response yourself.
     */
    default boolean preHandle(Request req) throws Exception {
        return true;
    }

    /**
     * Called after the route handler has returned a response.
     * The response object can be mutated (e.g., add headers).
     */
    default void postHandle(Request req, Response res) throws Exception {
    }

    /**
     * Always called after completion, regardless of exceptions.
     * exception is null if no exception occurred.
     */
    default void afterCompletion(Request req, Response res, Exception exception) {
    }
}
