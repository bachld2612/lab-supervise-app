package com.bachld.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationResult class.
 * Tests the success and failure factory methods and basic functionality.
 */
public class ValidationResultTest {

    @Test
    public void testSuccessCreatesValidResult() {
        ValidationResult result = ValidationResult.success();
        
        assertTrue(result.isValid(), "Success result should be valid");
        assertNull(result.getErrorMessage(), "Success result should have no error message");
    }

    @Test
    public void testFailureCreatesInvalidResult() {
        String errorMessage = "Validation failed";
        ValidationResult result = ValidationResult.failure(errorMessage);
        
        assertFalse(result.isValid(), "Failure result should be invalid");
        assertEquals(errorMessage, result.getErrorMessage(), "Failure result should contain the error message");
    }

    @Test
    public void testConstructorWithValidFlag() {
        ValidationResult validResult = new ValidationResult(true, null);
        assertTrue(validResult.isValid());
        assertNull(validResult.getErrorMessage());
        
        ValidationResult invalidResult = new ValidationResult(false, "Error");
        assertFalse(invalidResult.isValid());
        assertEquals("Error", invalidResult.getErrorMessage());
    }

    @Test
    public void testFailureWithEmptyMessage() {
        ValidationResult result = ValidationResult.failure("");
        
        assertFalse(result.isValid(), "Failure result should be invalid even with empty message");
        assertEquals("", result.getErrorMessage(), "Error message should be preserved as empty string");
    }

    @Test
    public void testFailureWithNullMessage() {
        ValidationResult result = ValidationResult.failure(null);
        
        assertFalse(result.isValid(), "Failure result should be invalid even with null message");
        assertNull(result.getErrorMessage(), "Null error message should be preserved");
    }
}
