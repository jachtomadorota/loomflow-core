package io.loomflow.security.apikey;

import io.loomflow.http.Request;
import io.loomflow.security.AuthenticationProvider;
import io.loomflow.security.Principal;
import io.loomflow.security.UnauthorizedException;

import java.util.Optional;
import java.util.function.Function;

/**
 * Authentication provider for API key authentication.
 *
 * Reads the API key from a configurable header (default: X-Api-Key)
 * or from a query parameter (default: disabled).
 *
 * The key is passed to a user-supplied resolver function that returns
 * the associated Principal, or empty if the key is unknown/invalid.
 *
 * Usage:
 * <pre>
 *   // From header X-Api-Key
 *   new ApiKeyAuthenticationProvider(key -> {
 *       if ("my-secret-key".equals(key)) {
 *           return Optional.of(Principal.builder("service-a").role("SERVICE").build());
 *       }
 *       return Optional.empty();
 *   });
 *
 *   // Custom header name
 *   ApiKeyAuthenticationProvider.builder()
 *       .headerName("X-Service-Token")
 *       .keyResolver(key -> resolveFromDatabase(key))
 *       .build();
 * </pre>
 */
public final class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final String headerName;
    private final String queryParamName;
    private final Function<String, Optional<Principal>> keyResolver;

    private ApiKeyAuthenticationProvider(Builder builder) {
        this.headerName = builder.headerName;
        this.queryParamName = builder.queryParamName;
        this.keyResolver = builder.keyResolver;
    }

    /** Convenience constructor using default header name "X-Api-Key". */
    public ApiKeyAuthenticationProvider(Function<String, Optional<Principal>> keyResolver) {
        this(ApiKeyAuthenticationProvider.builder().keyResolver(keyResolver));
    }

    @Override
    public boolean supports(Request request) {
        if (headerName != null && request.header(headerName) != null) {
            return true;
        }
        if (queryParamName != null && request.queryParam(queryParamName) != null) {
            return true;
        }
        return false;
    }

    @Override
    public Optional<Principal> authenticate(Request request) {
        String apiKey = extractKey(request);
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        Optional<Principal> principal = keyResolver.apply(apiKey);
        if (principal.isEmpty()) {
            throw new UnauthorizedException("Invalid or unknown API key");
        }
        return principal;
    }

    private String extractKey(Request request) {
        if (headerName != null) {
            String key = request.header(headerName);
            if (key != null) return key;
        }
        if (queryParamName != null) {
            return request.queryParam(queryParamName);
        }
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String headerName = "X-Api-Key";
        private String queryParamName = null;
        private Function<String, Optional<Principal>> keyResolver;

        /** Sets the header name to read the API key from. Default: X-Api-Key. */
        public Builder headerName(String headerName) {
            this.headerName = headerName;
            return this;
        }

        /**
         * Enables reading the API key from a query parameter (e.g. "api_key").
         * Disabled by default (header only).
         */
        public Builder queryParamName(String queryParamName) {
            this.queryParamName = queryParamName;
            return this;
        }

        /** Sets the function that resolves an API key string to a Principal. Required. */
        public Builder keyResolver(Function<String, Optional<Principal>> keyResolver) {
            this.keyResolver = keyResolver;
            return this;
        }

        public ApiKeyAuthenticationProvider build() {
            if (keyResolver == null) {
                throw new IllegalStateException("keyResolver is required");
            }
            return new ApiKeyAuthenticationProvider(this);
        }
    }
}
