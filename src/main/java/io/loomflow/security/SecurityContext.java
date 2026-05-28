package io.loomflow.security;

import java.util.concurrent.Callable;

/**
 * Thread-safe security context backed by Java 21 {@link ScopedValue}.
 *
 * ScopedValue is the correct choice for virtual threads — unlike ThreadLocal,
 * it does not leak across thread boundaries and is naturally scoped to
 * a structured concurrency task or a ScopedValue.where() block.
 *
 * Usage in middleware:
 * <pre>
 *   SecurityContext.run(principal, () -> handler.handle(request));
 *   Response r = SecurityContext.runAndReturn(principal, () -> handler.handle(request));
 * </pre>
 *
 * Usage in handler/service code:
 * <pre>
 *   Principal p = SecurityContext.current();
 *   SecurityContext.requireAuthenticated();
 *   SecurityContext.requireRole("ADMIN");
 * </pre>
 */
public final class SecurityContext {

    /** ScopedValue holding the current principal for the request scope. */
    static final ScopedValue<Principal> PRINCIPAL = ScopedValue.newInstance();

    private SecurityContext() {}

    /**
     * Runs the given action with the specified principal bound in scope.
     * The principal is automatically unbound when the action completes.
     *
     * @param principal the principal to bind
     * @param action    the action to run within the security context
     */
    public static void run(Principal principal, ThrowingRunnable action) {
            ScopedValue.where(PRINCIPAL, principal).run(() -> {
                try {
                    action.run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * Calls the given callable with the specified principal bound in scope.
     *
     * @param principal the principal to bind
     * @param callable  the callable to run
     * @param <T>       the return type
     * @return result of the callable
     * @throws Exception if callable throws
     */
    public static <T> T runAndReturn(Principal principal, Callable<T> callable) throws Exception {
        return ScopedValue.where(PRINCIPAL, principal).call(callable::call);
    }

    /**
     * Returns the current principal for this request scope.
     * Returns {@link Principal#ANONYMOUS} if no principal is bound.
     *
     * @return current principal, never null
     */
    public static Principal current() {
        return PRINCIPAL.orElse(Principal.ANONYMOUS);
    }

    /**
     * Returns true if a non-anonymous principal is bound in the current scope.
     */
    public static boolean isAuthenticated() {
        return PRINCIPAL.isBound() && !current().isAnonymous();
    }

    /**
     * Throws {@link UnauthorizedException} if the current principal is anonymous.
     */
    public static void requireAuthenticated() {
        if (!isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }
    }

    /**
     * Throws {@link ForbiddenException} if the current principal lacks the given role.
     */
    public static void requireRole(String role) {
        requireAuthenticated();
        if (!current().hasRole(role)) {
            throw new ForbiddenException("Role required: " + role);
        }
    }

    /**
     * Throws {@link ForbiddenException} if the current principal lacks any of the given roles.
     */
    public static void requireAnyRole(String... roles) {
        requireAuthenticated();
        if (!current().hasAnyRole(roles)) {
            throw new ForbiddenException("One of roles required: " + String.join(", ", roles));
        }
    }

    /**
     * Functional interface that allows throwing checked exceptions.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
