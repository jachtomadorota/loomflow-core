package io.loomflow.http;

/**
 * Handles an incoming HTTP request and returns a Response.
 * May throw any exception — the Router's ErrorHandler will catch it.
 */
@FunctionalInterface
public interface RouteHandler {
    Response handle(Request request) throws Exception;
}
