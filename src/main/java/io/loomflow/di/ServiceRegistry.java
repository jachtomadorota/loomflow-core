package io.loomflow.di;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Simple DI container — constructor injection, zero runtime reflection in hot path.
 *
 * <p>All wiring happens once at startup. Supports:
 * <ul>
 *   <li>Explicit instance binding: {@code bind(Type.class, instance)}</li>
 *   <li>Factory binding (lazy singleton): {@code bind(Type.class, () -> new Type(...))}</li>
 *   <li>Auto-wire by single constructor: {@code register(Type.class)}</li>
 *   <li>Interface → implementation: {@code register(Api.class, Impl.class)}</li>
 * </ul>
 */
public final class ServiceRegistry {

    private final Map<Class<?>, Object> singletons = new HashMap<>();
    private final Map<Class<?>, Supplier<?>> factories = new HashMap<>();

    // ── Binding ────────────────────────────────────────────────────────────────

    /** Bind an already-created instance. */
    public <T> ServiceRegistry bind(Class<T> type, T instance) {
        singletons.put(type, instance);
        return this;
    }

    /** Bind a lazy factory — evaluated once on first resolve. */
    public <T> ServiceRegistry bind(Class<T> type, Supplier<T> factory) {
        factories.put(type, factory);
        return this;
    }

    /** Auto-wire a class by its single constructor. */
    public <T> ServiceRegistry register(Class<T> type) {
        singletons.put(type, instantiate(type));
        return this;
    }

    /** Bind interface to implementation and auto-wire. */
    public <T, I extends T> ServiceRegistry register(Class<T> api, Class<I> impl) {
        I instance = instantiate(impl);
        singletons.put(api, instance);
        singletons.put(impl, instance);
        return this;
    }

    // ── Resolution ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type) {
        if (singletons.containsKey(type)) {
            return (T) singletons.get(type);
        }
        if (factories.containsKey(type)) {
            T instance = (T) factories.get(type).get();
            singletons.put(type, instance); // promote to singleton
            factories.remove(type);
            return instance;
        }
        throw new IllegalStateException("[LoomFlow DI] No binding for: " + type.getName());
    }

    public int size() {
        return singletons.size() + factories.size();
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> T instantiate(Class<T> type) {
        Constructor<?>[] ctors = type.getDeclaredConstructors();
        if (ctors.length != 1) {
            throw new IllegalStateException(
                    "[LoomFlow DI] " + type.getName() +
                    " must have exactly one constructor (found " + ctors.length + ")");
        }
        Constructor<T> ctor = (Constructor<T>) ctors[0];
        Object[] deps = Arrays.stream(ctor.getParameterTypes())
                .map(this::resolve)
                .toArray();
        try {
            return ctor.newInstance(deps);
        } catch (Exception e) {
            throw new RuntimeException("[LoomFlow DI] Could not instantiate: " + type.getName(), e);
        }
    }
}
