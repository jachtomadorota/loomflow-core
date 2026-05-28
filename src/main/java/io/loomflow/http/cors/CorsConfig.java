package io.loomflow.http.cors;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Immutable CORS configuration.
 * Build via CorsConfig.builder() or use CorsConfig.allowAll() for development.
 */
public final class CorsConfig {

    private final List<String> allowedOrigins;
    private final List<String> allowedMethods;
    private final List<String> allowedHeaders;
    private final boolean allowCredentials;
    private final long maxAge;

    private CorsConfig(Builder b) {
        this.allowedOrigins   = Collections.unmodifiableList(b.allowedOrigins);
        this.allowedMethods   = Collections.unmodifiableList(b.allowedMethods);
        this.allowedHeaders   = Collections.unmodifiableList(b.allowedHeaders);
        this.allowCredentials = b.allowCredentials;
        this.maxAge           = b.maxAge;
    }

    public static CorsConfig allowAll() {
        return builder()
                .allowOrigins("*")
                .allowMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                .allowHeaders("*")
                .maxAge(3600)
                .build();
    }

    public static Builder builder() { return new Builder(); }

    public boolean isOriginAllowed(String origin) {
        if (origin == null) return false;
        return allowedOrigins.contains("*") || allowedOrigins.contains(origin);
    }

    public List<String> allowedOrigins()   { return allowedOrigins; }
    public List<String> allowedMethods()   { return allowedMethods; }
    public List<String> allowedHeaders()   { return allowedHeaders; }
    public boolean      allowCredentials() { return allowCredentials; }
    public long         maxAge()           { return maxAge; }

    public static final class Builder {
        private List<String> allowedOrigins = List.of("*");
        private List<String> allowedMethods = List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private boolean allowCredentials    = false;
        private long maxAge                 = 1800;

        public Builder allowOrigins(String... origins) {
            this.allowedOrigins = Arrays.asList(origins); return this;
        }
        public Builder allowMethods(String... methods) {
            this.allowedMethods = Arrays.asList(methods); return this;
        }
        public Builder allowHeaders(String... headers) {
            this.allowedHeaders = Arrays.asList(headers); return this;
        }
        public Builder allowCredentials(boolean v) {
            this.allowCredentials = v; return this;
        }
        public Builder maxAge(long seconds) {
            this.maxAge = seconds; return this;
        }
        public CorsConfig build() { return new CorsConfig(this); }
    }
}
