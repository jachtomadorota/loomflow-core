package io.loomflow.security;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Represents an authenticated principal (user or service account).
 * Immutable value object — safe for use in virtual threads.
 *
 * @param id         unique identifier of the principal (e.g. user ID, API key ID)
 * @param name       display name or username
 * @param roles      set of roles granted to this principal
 * @param attributes additional metadata (e.g. email, tenant, claims from JWT)
 */
public record Principal(
        String id,
        String name,
        Set<String> roles,
        Map<String, Object> attributes
) {

    /** Anonymous principal — used when no authentication is present. */
    public static final Principal ANONYMOUS = new Principal(
            "anonymous", "anonymous", Set.of(), Map.of()
    );

    public Principal {
        roles = Collections.unmodifiableSet(Set.copyOf(roles));
        attributes = Collections.unmodifiableMap(Map.copyOf(attributes));
    }

    /** Returns true if this principal has the given role. */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /** Returns true if this principal has any of the given roles. */
    public boolean hasAnyRole(String... requiredRoles) {
        for (String role : requiredRoles) {
            if (roles.contains(role)) return true;
        }
        return false;
    }

    /** Returns true if this principal has all of the given roles. */
    public boolean hasAllRoles(String... requiredRoles) {
        for (String role : requiredRoles) {
            if (!roles.contains(role)) return false;
        }
        return true;
    }

    /** Returns true if this is the anonymous (unauthenticated) principal. */
    public boolean isAnonymous() {
        return this == ANONYMOUS || "anonymous".equals(id);
    }

    /** Returns true if this principal is authenticated (not anonymous). */
    public boolean isAuthenticated() {
        return !isAnonymous();
    }

    /** Typed attribute access. Returns null if absent or wrong type. */
    @SuppressWarnings("unchecked")
    public <T> T attribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * Builder for constructing Principal instances.
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String name;
        private final Set<String> roles = new java.util.LinkedHashSet<>();
        private final Map<String, Object> attributes = new java.util.LinkedHashMap<>();

        private Builder(String id) {
            this.id = id;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder role(String role) {
            this.roles.add(role);
            return this;
        }

        public Builder roles(Set<String> roles) {
            this.roles.addAll(roles);
            return this;
        }

        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        public Principal build() {
            return new Principal(
                    id,
                    name != null ? name : id,
                    Set.copyOf(roles),
                    Map.copyOf(attributes)
            );
        }
    }
}
