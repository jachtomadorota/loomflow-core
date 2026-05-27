package io.loomflow.core;

public record ServiceBinding<T, I extends T>(Class<T> api, Class<I> impl) {}
