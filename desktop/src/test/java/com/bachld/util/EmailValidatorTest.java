package com.bachld.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmailValidator class.
 * Tests email validation for empty, missing @, invalid format, and valid format cases.
 * 
 * Requirements: 4.1, 4.2, 4.3
 */
public class EmailValidatorTest {

    // Requirement 4.1: Empty email validation
    @Test
    public void testValidateRejectsEmptyEmail() {
        ValidationResult result = EmailValidator.validate("");
        
        assertFalse(result.isValid(), "Empty email should be invalid");
        assertEquals("Vui lòng nhập địa chỉ email.", result.getErrorMessage());
    }

    @Test
    public void testValidateRejectsNullEmail() {
        ValidationResult result = EmailValidator.validate(null);
        
        assertFalse(result.isValid(), "Null email should be invalid");
        assertEquals("Vui lòng nhập địa chỉ email.", result.getErrorMessage());
    }

    @Test
    public void testValidateRejectsWhitespaceOnlyEmail() {
        ValidationResult result = EmailValidator.validate("   ");
        
        assertFalse(result.isValid(), "Whitespace-only email should be invalid");
        assertEquals("Vui lòng nhập địa chỉ email.", result.getErrorMessage());
    }

    // Requirement 4.2: Missing @ symbol validation
    @Test
    public void testValidateRejectsEmailWithoutAtSymbol() {
        ValidationResult result = EmailValidator.validate("userexample.com");
        
        assertFalse(result.isValid(), "Email without @ should be invalid");
        assertEquals("Địa chỉ email không hợp lệ.", result.getErrorMessage());
    }

    @Test
    public void testValidateRejectsEmailWithOnlyLocalPart() {
        ValidationResult result = EmailValidator.validate("username");
        
        assertFalse(result.isValid(), "Email with only local part should be invalid");
        assertEquals("Địa chỉ email không hợp lệ.", result.getErrorMessage());
    }

    // Requirement 4.3: Invalid format validation
    @Test
    public void testValidateRejectsEmailWithoutDomain() {
        ValidationResult result = EmailValidator.validate("user@");
        
        assertFalse(result.isValid(), "Email without domain should be invalid");
        assertEquals("Địa chỉ email không hợp lệ.", result.getErrorMessage());
    }

    @Test
    public void testValidateRejectsEmailWithoutTld() {
        ValidationResult result = EmailValidator.validate("user@domain");
        
        assertFalse(result.isValid(), "Email without TLD should be invalid");
        assertEquals("Địa chỉ email không hợp lệ.", result.getErrorMessage());
    }

    @Test
    public void testValidateRejectsEmailWithInvalidCharacters() {
        ValidationResult result = EmailValidator.validate("user name@example.com");
        
        assertFalse(result.isValid(), "Email with spaces should be invalid");
        assertEquals("Địa chỉ email không hợp lệ.", result.getErrorMessage());
    }

    @Test
    public void testValidateRejectsEmailWithMultipleAtSymbols() {
        ValidationResult result = EmailValidator.validate("user@@example.com");
        
        assertFalse(result.isValid(), "Email with multiple @ symbols should be invalid");
        assertEquals("Địa chỉ email không hợp lệ.", result.getErrorMessage());
    }

    @Test
    public void testValidateRejectsEmailWithTooShortTld() {
        ValidationResult result = EmailValidator.validate("user@example.c");
        
        assertFalse(result.isValid(), "Email with single-character TLD should be invalid");
        assertEquals("Địa chỉ email không hợp lệ.", result.getErrorMessage());
    }

    // Valid email format tests
    @Test
    public void testValidateAcceptsSimpleValidEmail() {
        ValidationResult result = EmailValidator.validate("user@example.com");
        
        assertTrue(result.isValid(), "Simple valid email should be accepted");
        assertNull(result.getErrorMessage(), "Valid email should have no error message");
    }

    @Test
    public void testValidateAcceptsEmailWithDots() {
        ValidationResult result = EmailValidator.validate("first.last@example.com");
        
        assertTrue(result.isValid(), "Email with dots in local part should be accepted");
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testValidateAcceptsEmailWithUnderscore() {
        ValidationResult result = EmailValidator.validate("user_name@example.com");
        
        assertTrue(result.isValid(), "Email with underscore should be accepted");
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testValidateAcceptsEmailWithPlus() {
        ValidationResult result = EmailValidator.validate("user+tag@example.com");
        
        assertTrue(result.isValid(), "Email with plus sign should be accepted");
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testValidateAcceptsEmailWithHyphen() {
        ValidationResult result = EmailValidator.validate("user-name@example.com");
        
        assertTrue(result.isValid(), "Email with hyphen should be accepted");
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testValidateAcceptsEmailWithSubdomain() {
        ValidationResult result = EmailValidator.validate("user@mail.example.com");
        
        assertTrue(result.isValid(), "Email with subdomain should be accepted");
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testValidateAcceptsEmailWithLongTld() {
        ValidationResult result = EmailValidator.validate("user@example.co.uk");
        
        assertTrue(result.isValid(), "Email with multi-part TLD should be accepted");
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testValidateAcceptsEmailWithNumbers() {
        ValidationResult result = EmailValidator.validate("user123@example456.com");
        
        assertTrue(result.isValid(), "Email with numbers should be accepted");
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testValidateAcceptsEmailWithMixedCase() {
        ValidationResult result = EmailValidator.validate("User@Example.COM");
        
        assertTrue(result.isValid(), "Email with mixed case should be accepted");
        assertNull(result.getErrorMessage());
    }
}
