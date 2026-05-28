package io.loomflow.security;

import io.loomflow.http.LoomFlowException;

/**
 * Thrown when a request cannot be authenticated.
 * Maps to HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends LoomFlowException {

    public UnauthorizedException(String message) {
        super(401, message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(401, message, cause);
    }
}
