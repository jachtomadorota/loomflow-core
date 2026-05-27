package io.loomflow.core;

import io.loomflow.http.RouteHandler;

public record RouteDef(String method, String path, RouteHandler handler) {}
