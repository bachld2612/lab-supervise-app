package com.bachld.client;

import com.bachld.config.RestClient;
import com.bachld.exception.RestClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthApiClientTest {

    private AuthApiClient authApiClient;

    @BeforeEach
    void setUp() {
        authApiClient = new AuthApiClient(RestClient.getInstance());
    }

    @Test
    void authApiClient_ShouldBeInstantiatedWithRestClient() {
        assertNotNull(authApiClient);
    }

    @Test
    void login_WithNullEmail_ThrowsRestClientException() {
        RestClientException ex = assertThrows(RestClientException.class,
                () -> authApiClient.login(null, "password123", "desktop", null));
        assertTrue(ex.getMessage().contains("Failed to authenticate"));
    }

    @Test
    void login_WithEmptyPassword_ThrowsRestClientException() {
        RestClientException ex = assertThrows(RestClientException.class,
                () -> authApiClient.login("test@example.com", "", "desktop", null));
        assertTrue(ex.getMessage().contains("Failed to authenticate"));
    }

    @Test
    void login_WithNullDevice_ThrowsRestClientException() {
        RestClientException ex = assertThrows(RestClientException.class,
                () -> authApiClient.login("test@example.com", "password123", null, null));
        assertTrue(ex.getMessage().contains("Failed to authenticate"));
    }

    @Test
    void login_MethodSignature_IsCorrect() {
        try {
            authApiClient.login("test@example.com", "password123", "desktop", null);
            fail("Should throw RestClientException when server is not available");
        } catch (RestClientException e) {
            assertTrue(e.getMessage().contains("Failed to authenticate"));
        }
    }
}
