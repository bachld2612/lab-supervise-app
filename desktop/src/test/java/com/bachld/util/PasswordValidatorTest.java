package com.bachld.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PasswordValidator class.
 * Tests password validation for empty and too-short passwords.
 * 
 * Requirements: 5.1, 5.2
 */
public class PasswordValidatorTest {

    // Requirement 5.1: Empty password validation
    @Test
    public void testValidateRejectsEmptyPassword() {
        ValidationResult result = PasswordValidator.validate("");
        
        assertFalse(result.isValid(), "Empty password should be invalid");
        assertEquals("Vui lòng nhập mật khẩu.", result.getErrorMessage());
    }

    @Test
    public void testValidateRejectsNullPassword() {
        ValidationResult result = PasswordValidator.validate(null);
        
        assertFalse(result.isValid(), "Null password should be invalid");
        assertEquals("Vui lòng nhập mật khẩu.", result.getErrorMessage());
    }

    // Requirement 5.2: Too-short password validation
    @Test
    public void testValidateRejectsOneCharacterPassword() {
        ValidationResult result = PasswordValidator.validate("a");
        
        assertFalse(result.isValid(), "One-character password should be invalid");
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự.", result.getErrorMessage());
    }

    @Test
    public void testValidateRejectsTwoCharacterPassword() {
        ValidationResult result = PasswordValidator.validate("ab");
        
        assertFalse(result.isValid(), "Two-character password should be invalid");
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự.", result.getErrorMessage());
    }

    @Test
    public void testValidateRejectsThreeCharacterPassword() {
        ValidationResult result = PasswordValidator.validate("abc");
        
        assertFalse(result.isValid(), "Three-character password should be invalid");
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự.", result.getErrorMessage());
    }

    @Test
    public void testValidateRejectsFourCharacterPassword() {
        ValidationResult result = PasswordValidator.validate("abcd");
        
        assertFalse(result.isValid(), "Four-character password should be invalid");
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự.", result.getErrorMessage());
    }

    @Test
    public void testValidateRejectsFiveCharacterPassword() {
        ValidationResult result = PasswordValidator.validate("abcde");
        
        assertFalse(result.isValid(), "Five-character password should be invalid");
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự.", result.getErrorMessage());
    }

    // Valid password tests
    @Test
    public void testValidateAcceptsSixCharacterPassword() {
        ValidationResult result = PasswordValidator.validate("abcdef");
        
        assertTrue(result.isValid(), "Six-character password should be valid");
        assertNull(result.getErrorMessage(), "Valid password should have no error message");
    }

    @Test
    public void testValidateAcceptsSevenCharacterPassword() {
        ValidationResult result = PasswordValidator.validate("abcdefg");
        
        assertTrue(result.isValid(), "Seven-character password should be valid");
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testValidateAcceptsLongPassword() {
        ValidationResult result = PasswordValidator.validate("thisIsAVeryLongPassword123!");
        
        assertTrue(result.isValid(), "Long password should be valid");
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testValidateAcceptsPasswordWithNumbers() {
        ValidationResult result = PasswordValidator.validate("pass123");
        
        assertTrue(result.isValid(), "Password with numbers should be valid");
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testValidateAcceptsPasswordWithSpecialCharacters() {
        ValidationResult result = PasswordValidator.validate("p@ss!#");
        
        assertTrue(result.isValid(), "Password with special characters should be valid");
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testValidateAcceptsPasswordWithSpaces() {
        ValidationResult result = PasswordValidator.validate("pass word");
        
        assertTrue(result.isValid(), "Password with spaces should be valid");
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testValidateAcceptsPasswordWithMixedCase() {
        ValidationResult result = PasswordValidator.validate("PaSsWoRd");
        
        assertTrue(result.isValid(), "Password with mixed case should be valid");
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testValidateAcceptsPasswordWithUnicode() {
        ValidationResult result = PasswordValidator.validate("mật_khẩu");
        
        assertTrue(result.isValid(), "Password with Unicode characters should be valid");
        assertNull(result.getErrorMessage());
    }
}
