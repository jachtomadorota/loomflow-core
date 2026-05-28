package io.loomflow.security.basic;

import io.loomflow.http.Request;
import io.loomflow.security.AuthenticationProvider;
import io.loomflow.security.Principal;
import io.loomflow.security.UnauthorizedException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Authentication provider for HTTP Basic Authentication (RFC 7617).
 *
 * Decodes the Base64-encoded "username:password" from the Authorization header
 * and delegates credential verification to a user-supplied function.
 *
 * Usage:
 * <pre>
 *   new BasicAuthenticationProvider((username, password) -> {
 *       if ("admin".equals(username) && "secret".equals(password)) {
 *           return Optional.of(Principal.builder(username)
 *               .role("ADMIN").build());
 *       }
 *       return Optional.empty();
 *   });
 * </pre>
 */
public final class BasicAuthenticationProvider implements AuthenticationProvider {

    private static final String BASIC_PREFIX = "Basic ";

    /**
     * Function that receives (username, password) and returns an authenticated
     * Principal, or empty Optional if credentials are invalid.
     */
    private final BiFunction<String, String, Optional<Principal>> credentialVerifier;

    public BasicAuthenticationProvider(BiFunction<String, String, Optional<Principal>> credentialVerifier) {
        this.credentialVerifier = credentialVerifier;
    }

    @Override
    public boolean supports(Request request) {
        String authHeader = request.header("Authorization");
        return authHeader != null && authHeader.startsWith(BASIC_PREFIX);
    }

    @Override
    public Optional<Principal> authenticate(Request request) {
        String authHeader = request.header("Authorization");
        if (authHeader == null || !authHeader.startsWith(BASIC_PREFIX)) {
            return Optional.empty();
        }

        String encoded = authHeader.substring(BASIC_PREFIX.length()).trim();
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid Basic Auth encoding");
        }

        int colonIndex = decoded.indexOf(':');
        if (colonIndex < 0) {
            throw new UnauthorizedException("Invalid Basic Auth format: missing colon");
        }

        String username = decoded.substring(0, colonIndex);
        String password = decoded.substring(colonIndex + 1);

        Optional<Principal> principal = credentialVerifier.apply(username, password);
        if (principal.isEmpty()) {
            throw new UnauthorizedException("Invalid credentials");
        }
        return principal;
    }
}
