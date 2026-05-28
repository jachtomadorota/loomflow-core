package io.loomflow.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.loomflow.http.Request;
import io.loomflow.security.AuthenticationProvider;
import io.loomflow.security.Principal;
import io.loomflow.security.UnauthorizedException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Authentication provider that validates Bearer JWT tokens from the Authorization header.
 *
 * Tokens are verified with HMAC-SHA256 (HS256) using the provided secret key.
 * Claims extracted: sub (→ id), name, roles (array claim), and remaining claims as attributes.
 *
 * Usage:
 * <pre>
 *   new JwtAuthenticationProvider("my-secret-key-at-least-32-chars!!")
 * </pre>
 */
public final class JwtAuthenticationProvider implements AuthenticationProvider {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLES_CLAIM = "roles";

    private final SecretKey signingKey;
    private final String issuer;

    /**
     * Creates a provider that validates HS256 JWTs signed with the given secret.
     *
     * @param secret the HMAC secret (must be at least 32 characters for HS256)
     */
    public JwtAuthenticationProvider(String secret) {
        this(secret, null);
    }

    /**
     * Creates a provider that validates HS256 JWTs, additionally verifying the issuer.
     *
     * @param secret the HMAC secret
     * @param issuer expected issuer claim value, or null to skip issuer validation
     */
    public JwtAuthenticationProvider(String secret, String issuer) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    @Override
    public boolean supports(Request request) {
        String authHeader = request.header("Authorization");
        return authHeader != null && authHeader.startsWith(BEARER_PREFIX);
    }

    @Override
    public Optional<Principal> authenticate(Request request) {
        String authHeader = request.header("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            var parserBuilder = Jwts.parser().verifyWith(signingKey);
            if (issuer != null) {
                parserBuilder.requireIssuer(issuer);
            }
            Claims claims = parserBuilder.build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new UnauthorizedException("JWT missing subject claim");
            }

            // Extract roles
            Set<String> roles = new LinkedHashSet<>();
            Object rolesClaim = claims.get(ROLES_CLAIM);
            if (rolesClaim instanceof Collection<?> rolesList) {
                for (Object role : rolesList) {
                    if (role instanceof String s) roles.add(s);
                }
            }

            // Build principal with all non-standard claims as attributes
            String name = claims.get("name", String.class);
            Principal.Builder builder = Principal.builder(subject)
                    .name(name != null ? name : subject)
                    .roles(Set.copyOf(roles));

            // Add remaining claims as attributes (skip JWT standard ones)
            Set<String> skip = Set.of("sub", ROLES_CLAIM, "iat", "exp", "iss", "nbf", "jti", "name");
            for (Map.Entry<String, Object> entry : claims.entrySet()) {
                if (!skip.contains(entry.getKey())) {
                    builder.attribute(entry.getKey(), entry.getValue());
                }
            }
            if (claims.getExpiration() != null) {
                builder.attribute("exp", claims.getExpiration().getTime());
            }

            return Optional.of(builder.build());

        } catch (JwtException e) {
            throw new UnauthorizedException("Invalid JWT token: " + e.getMessage(), e);
        }
    }
}
