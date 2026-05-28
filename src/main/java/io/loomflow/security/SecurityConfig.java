package io.loomflow.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable security configuration holding authentication providers,
 * authorization policy, and optional exclusion paths.
 *
 * Build via {@link SecurityConfig#builder()}:
 * <pre>
 *   SecurityConfig config = SecurityConfig.builder()
 *       .provider(new JwtAuthenticationProvider(secret))
 *       .policy(AuthorizationPolicy.authenticated())
 *       .exclude("/health", "/metrics")
 *       .build();
 * </pre>
 */
public final class SecurityConfig {

    private final List<AuthenticationProvider> providers;
    private final AuthorizationPolicy policy;
    private final List<String> excludedPaths;
    private final boolean allowAnonymous;

    private SecurityConfig(Builder builder) {
        this.providers = Collections.unmodifiableList(new ArrayList<>(builder.providers));
        this.policy = builder.policy;
        this.excludedPaths = Collections.unmodifiableList(new ArrayList<>(builder.excludedPaths));
        this.allowAnonymous = builder.allowAnonymous;
    }

    public List<AuthenticationProvider> providers() {
        return providers;
    }

    public AuthorizationPolicy policy() {
        return policy;
    }

    public List<String> excludedPaths() {
        return excludedPaths;
    }

    public boolean allowAnonymous() {
        return allowAnonymous;
    }

    /**
     * Returns true if the given path is excluded from security enforcement.
     */
    public boolean isExcluded(String path) {
        for (String excluded : excludedPaths) {
            if (excluded.endsWith("/**")) {
                String prefix = excluded.substring(0, excluded.length() - 3);
                if (path.startsWith(prefix)) return true;
            } else if (excluded.equals(path)) {
                return true;
            }
        }
        return false;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<AuthenticationProvider> providers = new ArrayList<>();
        private AuthorizationPolicy policy = AuthorizationPolicy.authenticated();
        private final List<String> excludedPaths = new ArrayList<>();
        private boolean allowAnonymous = false;

        /** Adds an authentication provider. Providers are tried in registration order. */
        public Builder provider(AuthenticationProvider provider) {
            this.providers.add(provider);
            return this;
        }

        /**
         * Sets the global authorization policy.
         * Defaults to {@link AuthorizationPolicy#authenticated()}.
         */
        public Builder policy(AuthorizationPolicy policy) {
            this.policy = policy;
            return this;
        }

        /**
         * Excludes specific paths from authentication/authorization.
         * Supports exact paths and prefix wildcards ending with /**.
         * Example: "/health", "/public/**"
         */
        public Builder exclude(String... paths) {
            for (String path : paths) {
                this.excludedPaths.add(path);
            }
            return this;
        }

        /**
         * When true, requests that fail all authentication providers are passed through
         * as {@link Principal#ANONYMOUS} (policy still runs).
         * When false (default), unauthenticated requests throw {@link UnauthorizedException}.
         */
        public Builder allowAnonymous(boolean allow) {
            this.allowAnonymous = allow;
            return this;
        }

        public SecurityConfig build() {
            return new SecurityConfig(this);
        }
    }
}
