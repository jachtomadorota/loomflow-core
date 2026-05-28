package io.loomflow.security;

import io.loomflow.http.Request;

import java.util.Optional;

/**
 * Strategy interface for authenticating incoming HTTP requests.
 *
 * Implementations extract credentials from the request (e.g. Authorization header,
 * query parameter, cookie) and resolve them to a {@link Principal}.
 *
 * Multiple providers can be registered; they are tried in order until one succeeds.
 * If no provider can handle the request, the result is {@link Optional#empty()}.
 *
 * Implementations must be stateless and thread-safe (virtual thread friendly).
 */
public interface AuthenticationProvider {

    /**
     * Determines whether this provider can process the given request.
     * Called before {@link #authenticate(Request)} to avoid unnecessary work.
     *
     * @param request the incoming HTTP request
     * @return true if this provider should attempt authentication
     */
    boolean supports(Request request);

    /**
     * Attempts to authenticate the request and returns the resolved principal.
     *
     * @param request the incoming HTTP request
     * @return an Optional containing the authenticated Principal,
     *         or empty if authentication failed (invalid credentials)
     * @throws UnauthorizedException if credentials are present but invalid
     */
    Optional<Principal> authenticate(Request request);
}
