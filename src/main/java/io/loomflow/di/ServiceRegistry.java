package io.loomflow.di;

import java.util.HashMap;
import java.util.Map;

public final class ServiceRegistry {
    private final Map<Class<?>, Class<?>> bindings = new HashMap<>();

    public <T> void register(Class<?> api, Class<?> impl) {
        bindings.put(api, impl);
    }

    public int size() {
        return bindings.size();
    }
}
