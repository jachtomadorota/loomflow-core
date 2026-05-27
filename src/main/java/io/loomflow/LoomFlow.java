package io.loomflow;

import io.loomflow.core.AppBuilder;

public final class LoomFlow {
    private LoomFlow() {}

    public static AppBuilder app() {
        return new AppBuilder();
    }
}
