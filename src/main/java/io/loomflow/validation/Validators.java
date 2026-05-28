package io.loomflow.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import java.util.List;
import java.util.Set;

/**
 * Explicitly-called validation utility backed by Hibernate Validator.
 * No AOP, no proxy, no magic — call validate() where you need it.
 *
 * Usage:
 *   Validators.validate(myDto);  // throws ValidationException on failure
 *   List<ViolationError> errors = Validators.check(myDto);  // returns errors without throwing
 */
public final class Validators {

    private static final ValidatorFactory FACTORY =
            Validation.buildDefaultValidatorFactory();

    private static final jakarta.validation.Validator VALIDATOR =
            FACTORY.getValidator();

    private Validators() {}

    /**
     * Validates the given object against Bean Validation constraints.
     * Throws {@link ValidationException} if any constraint is violated.
     *
     * @param object the object to validate (must not be null)
     * @param <T>    the type of the object
     * @throws ValidationException if validation fails
     */
    public static <T> void validate(T object) {
        List<ValidationException.ViolationError> errors = check(object);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /**
     * Validates the given object and returns a list of violation errors.
     * Returns an empty list if the object is valid.
     *
     * @param object the object to validate (must not be null)
     * @param <T>    the type of the object
     * @return list of violations, empty if valid
     */
    public static <T> List<ValidationException.ViolationError> check(T object) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(object);
        return violations.stream()
                .map(v -> new ValidationException.ViolationError(
                        v.getPropertyPath().toString(),
                        v.getMessage()))
                .sorted(java.util.Comparator.comparing(ValidationException.ViolationError::field))
                .toList();
    }

    /**
     * Validates the given object against specific validation groups.
     * Throws {@link ValidationException} if any constraint is violated.
     *
     * @param object the object to validate
     * @param groups validation groups to apply
     * @param <T>    the type of the object
     * @throws ValidationException if validation fails
     */
    public static <T> void validateGroups(T object, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(object, groups);
        if (!violations.isEmpty()) {
            List<ValidationException.ViolationError> errors = violations.stream()
                    .map(v -> new ValidationException.ViolationError(
                            v.getPropertyPath().toString(),
                            v.getMessage()))
                    .sorted(java.util.Comparator.comparing(ValidationException.ViolationError::field))
                    .toList();
            throw new ValidationException(errors);
        }
    }
}
