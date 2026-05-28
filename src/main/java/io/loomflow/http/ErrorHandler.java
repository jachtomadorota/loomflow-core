package io.loomflow.http;

/**
 * Global error handler — called when a route handler or middleware throws an exception.
 */
@FunctionalInterface
public interface ErrorHandler {
    Response handle(Request request, Exception exception);
}
