package io.loomflow.data;

/**
 * Unchecked exception wrapping SQL and data access errors.
 * Eliminates checked SQLException propagation from data layer code.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
