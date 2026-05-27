package io.loomflow.http;

@FunctionalInterface
public interface RouteHandler {
    Response handle(Request request);
}
