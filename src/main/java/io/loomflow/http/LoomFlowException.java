package io.loomflow.http;

/**
 * Base unchecked exception for LoomFlow framework errors.
 * Carries an HTTP status code so error handlers can respond correctly.
 */
public class LoomFlowException extends RuntimeException {

    private final int statusCode;

    /** Creates an exception with a specific HTTP status code. */
    public LoomFlowException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /** Creates an exception with a specific HTTP status code and cause. */
    public LoomFlowException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /** Creates an exception with default status code 500. */
    public LoomFlowException(String message) {
        this(500, message);
    }

    /** Creates an exception with default status code 500 and cause. */
    public LoomFlowException(String message, Throwable cause) {
        this(500, message, cause);
    }

    /** Returns the HTTP status code associated with this exception. */
    public int statusCode() {
        return statusCode;
    }
}
