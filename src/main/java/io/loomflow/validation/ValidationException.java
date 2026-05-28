package io.loomflow.validation;

import java.util.List;

/**
 * Thrown when bean validation fails.
 * Auto-mapped to HTTP 400 by the default error handler.
 */
public final class ValidationException extends RuntimeException {

    private final List<ViolationError> violations;

    public ValidationException(List<ViolationError> violations) {
        super("Validation failed: " + violations.size() + " error(s)");
        this.violations = List.copyOf(violations);
    }

    public List<ViolationError> violations() { return violations; }

    /** Simple DTO for a single constraint violation. */
    public record ViolationError(String field, String message) {}
}
