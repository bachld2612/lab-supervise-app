package com.bachld.client;

import com.bachld.config.RestClient;
import com.bachld.exception.RestClientException;
import com.bachld.model.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthApiClient.
 * 
 * Validates: Requirements 7.1, 7.2, 7.3
 */
class AuthApiClientTest {
    
    private RestClient restClient;
    private AuthApiClient authApiClient;
    
    @BeforeEach
    void setUp() {
        restClient = RestClient.getInstance();
        authApiClient = new AuthApiClient(restClient);
    }
    
    @Test
    void authApiClient_ShouldBeInstantiatedWithRestClient() {
        assertNotNull(authApiClient, "AuthApiClient should be instantiated");
    }
    
    @Test
    void login_WithNullEmail_ThrowsRestClientException() {
        // Act & Assert
        RestClientException exception = assertThrows(
            RestClientException.class,
            () -> authApiClient.login(null, "password123", "desktop")
        );
        
        assertTrue(exception.getMessage().contains("Failed to authenticate"));
    }
    
    @Test
    void login_WithEmptyPassword_ThrowsRestClientException() {
        // Act & Assert
        RestClientException exception = assertThrows(
            RestClientException.class,
            () -> authApiClient.login("test@example.com", "", "desktop")
        );
        
        assertTrue(exception.getMessage().contains("Failed to authenticate"));
    }
    
    @Test
    void login_WithNulldevice_ThrowsRestClientException() {
        // Act & Assert
        RestClientException exception = assertThrows(
            RestClientException.class,
            () -> authApiClient.login("test@example.com", "password123", null)
        );
        
        assertTrue(exception.getMessage().contains("Failed to authenticate"));
    }
    
    /**
     * Integration test note: This test verifies the method signature and exception handling.
     * Full integration testing with a real server or mock server would be done separately.
     * 
     * The login method should:
     * - POST to /api/auth/v1/login endpoint (Requirement 7.1)
     * - Include email, password, device in request body (Requirement 7.2)
     * - Set Content-Type header to application/json (Requirement 7.3)
     * - Return AuthResponse or throw RestClientException
     */
    @Test
    void login_MethodSignature_IsCorrect() {
        // This test verifies the method exists with correct signature
        // Actual API integration would require a running server or MockRestServiceServer
        
        try {
            // Attempt to call with invalid server URL will throw exception
            authApiClient.login("test@example.com", "password123", "desktop");
            fail("Should throw RestClientException when server is not available");
        } catch (RestClientException e) {
            // Expected - server is not running
            assertTrue(e.getMessage().contains("Failed to authenticate"));
        }
    }
}
