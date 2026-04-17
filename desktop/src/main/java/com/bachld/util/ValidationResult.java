package com.bachld.util;

/**
 * Represents the result of a validation operation.
 * Contains a boolean flag indicating validity and an optional error message.
 * 
 * Requirements: 4.4, 5.3
 */
public class ValidationResult {
    private final boolean valid;
    private final String errorMessage;

    /**
     * Constructs a ValidationResult with the specified validity and error message.
     *
     * @param valid true if validation passed, false otherwise
     * @param errorMessage the error message if validation failed, null if valid
     */
    public ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    /**
     * Checks if the validation was successful.
     *
     * @return true if validation passed, false otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Gets the error message if validation failed.
     *
     * @return the error message, or null if validation passed
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Creates a successful validation result.
     *
     * @return a ValidationResult indicating success
     */
    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    /**
     * Creates a failed validation result with the specified error message.
     *
     * @param message the error message describing why validation failed
     * @return a ValidationResult indicating failure
     */
    public static ValidationResult failure(String message) {
        return new ValidationResult(false, message);
    }
}
