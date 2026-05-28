package io.loomflow.security;

import io.loomflow.http.Middleware;
import io.loomflow.http.Request;
import io.loomflow.http.Response;
import io.loomflow.http.RouteHandler;

import java.util.Optional;

/**
 * Middleware that enforces authentication and authorization on every request.
 *
 * Pipeline:
 * 1. Check if path is excluded → pass through if yes
 * 2. Try each AuthenticationProvider in order → resolve Principal
 * 3. If no provider matches and allowAnonymous=false → throw UnauthorizedException
 * 4. Bind principal to SecurityContext via ScopedValue
 * 5. Evaluate AuthorizationPolicy → throw ForbiddenException if denied
 * 6. Call next handler within the security scope
 *
 * No AOP, no proxy, no annotation scanning. Explicit and composable.
 */
public final class SecurityMiddleware implements Middleware {

    private final SecurityConfig config;

    public SecurityMiddleware(SecurityConfig config) {
        this.config = config;
    }

    @Override
    public Response apply(Request request, RouteHandler next) throws Exception {
        // 1. Check exclusions
        if (config.isExcluded(request.path())) {
            return next.handle(request);
        }

        // 2. Resolve principal via providers
        Principal principal = resolvePrincipal(request);

        // 3. Evaluate authorization policy
        if (!config.policy().isAllowed(principal, request)) {
            if (principal.isAnonymous()) {
                throw new UnauthorizedException("Authentication required");
            }
            throw new ForbiddenException("Access denied for principal: " + principal.name());
        }

        // 4. Run handler within security scope (ScopedValue bound)
        return SecurityContext.runAndReturn(principal, () -> next.handle(request));
    }

    private Principal resolvePrincipal(Request request) {
        for (AuthenticationProvider provider : config.providers()) {
            if (!provider.supports(request)) {
                continue;
            }
            Optional<Principal> result = provider.authenticate(request);
            if (result.isPresent()) {
                return result.get();
            }
        }

        // No provider matched
        if (!config.allowAnonymous()) {
            throw new UnauthorizedException("No valid credentials provided");
        }
        return Principal.ANONYMOUS;
    }
}
