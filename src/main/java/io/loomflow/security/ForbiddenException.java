package io.loomflow.security;

import io.loomflow.http.LoomFlowException;

/**
 * Thrown when an authenticated principal lacks permission to access a resource.
 * Maps to HTTP 403 Forbidden.
 */
public class ForbiddenException extends LoomFlowException {

    public ForbiddenException(String message) {
        super(403, message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(403, message, cause);
    }
}
