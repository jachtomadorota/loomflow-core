package io.loomflow.security;

import io.loomflow.http.Request;

/**
 * Strategy interface for authorizing requests based on the authenticated principal.
 *
 * Authorization runs AFTER authentication. The principal is resolved
 * from {@link SecurityContext} by the framework before calling this policy.
 *
 * Implementations should be stateless and fast (no I/O if possible).
 */
@FunctionalInterface
public interface AuthorizationPolicy {

    /**
     * Decides whether the given principal is allowed to perform the request.
     *
     * @param principal the authenticated principal (never null; may be ANONYMOUS)
     * @param request   the incoming HTTP request
     * @return true if access is granted, false if access is denied
     */
    boolean isAllowed(Principal principal, Request request);

    // -------------------------------------------------------------------------
    // Built-in factory methods
    // -------------------------------------------------------------------------

    /** Grants access to all requests, including anonymous. */
    static AuthorizationPolicy permitAll() {
        return (principal, request) -> true;
    }

    /** Denies all requests. */
    static AuthorizationPolicy denyAll() {
        return (principal, request) -> false;
    }

    /** Requires the principal to be authenticated (not anonymous). */
    static AuthorizationPolicy authenticated() {
        return (principal, request) -> principal.isAuthenticated();
    }

    /** Requires the principal to have the specified role. */
    static AuthorizationPolicy requireRole(String role) {
        return (principal, request) -> principal.hasRole(role);
    }

    /** Requires the principal to have any of the specified roles. */
    static AuthorizationPolicy requireAnyRole(String... roles) {
        return (principal, request) -> principal.hasAnyRole(roles);
    }

    /** Requires the principal to have all of the specified roles. */
    static AuthorizationPolicy requireAllRoles(String... roles) {
        return (principal, request) -> principal.hasAllRoles(roles);
    }

    /**
     * Composes this policy with another: both must pass (logical AND).
     *
     * @param other the other policy
     * @return a combined policy
     */
    default AuthorizationPolicy and(AuthorizationPolicy other) {
        return (principal, request) -> this.isAllowed(principal, request)
                && other.isAllowed(principal, request);
    }

    /**
     * Composes this policy with another: at least one must pass (logical OR).
     *
     * @param other the other policy
     * @return a combined policy
     */
    default AuthorizationPolicy or(AuthorizationPolicy other) {
        return (principal, request) -> this.isAllowed(principal, request)
                || other.isAllowed(principal, request);
    }
}
