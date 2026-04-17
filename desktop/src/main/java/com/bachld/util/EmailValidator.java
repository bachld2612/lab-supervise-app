package com.bachld.util;

import java.util.regex.Pattern;

/**
 * Validates email addresses according to RFC 5322 format.
 * Provides validation for empty emails, missing @ symbols, and invalid formats.
 * 
 * Requirements: 4.1, 4.2, 4.3
 */
public class EmailValidator {
    
    /**
     * Regex pattern for basic RFC 5322 email format validation.
     * Matches: [local-part]@[domain].[tld]
     * - Local part: alphanumeric, dots, underscores, plus signs, hyphens
     * - Domain: alphanumeric, dots, hyphens
     * - TLD: at least 2 characters
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * Validates an email address.
     *
     * @param email the email address to validate
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validate(String email) {
        // Requirement 4.1: Reject empty email
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.failure("Vui lòng nhập địa chỉ email.");
        }

        // Requirement 4.2: Reject email without @ symbol
        if (!email.contains("@")) {
            return ValidationResult.failure("Địa chỉ email không hợp lệ.");
        }

        // Requirement 4.3: Reject email not matching standard format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult.failure("Địa chỉ email không hợp lệ.");
        }

        return ValidationResult.success();
    }
}
