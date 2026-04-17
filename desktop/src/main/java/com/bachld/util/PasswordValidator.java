package com.bachld.util;

/**
 * Validates passwords according to minimum length requirements.
 * Provides validation for empty passwords and passwords that are too short.
 * 
 * Requirements: 5.1, 5.2
 */
public class PasswordValidator {
    
    /**
     * Minimum required password length.
     */
    private static final int MIN_LENGTH = 6;

    /**
     * Validates a password.
     *
     * @param password the password to validate
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validate(String password) {
        // Requirement 5.1: Reject empty password
        if (password == null || password.isEmpty()) {
            return ValidationResult.failure("Vui lòng nhập mật khẩu.");
        }

        // Requirement 5.2: Reject password shorter than 6 characters
        if (password.length() < MIN_LENGTH) {
            return ValidationResult.failure("Mật khẩu phải có ít nhất 6 ký tự.");
        }

        return ValidationResult.success();
    }
}
