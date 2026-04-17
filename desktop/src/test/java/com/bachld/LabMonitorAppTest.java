package com.bachld;

import com.bachld.config.RestClient;
import com.bachld.service.SessionManager;
import com.bachld.service.TokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LabMonitorApp initialization
 * Validates: Requirements 7.4, 8.1, 9.1
 */
class LabMonitorAppTest {

    @BeforeEach
    void setUp() {
        // Clear singletons before each test to ensure clean state
        TokenManager.getInstance().clearToken();
        SessionManager.getInstance().clearSession();
    }

    @Test
    void testSingletonInitialization() {
        // Verify RestClient singleton can be initialized
        RestClient restClient = RestClient.getInstance();
        assertNotNull(restClient, "RestClient should be initialized");
        assertNotNull(restClient.getRestTemplate(), "RestTemplate should be configured");
        assertNotNull(restClient.getBaseUrl(), "Base URL should be configured");
        
        // Verify TokenManager singleton can be initialized
        TokenManager tokenManager = TokenManager.getInstance();
        assertNotNull(tokenManager, "TokenManager should be initialized");
        assertFalse(tokenManager.hasToken(), "TokenManager should start with no token");
        
        // Verify SessionManager singleton can be initialized
        SessionManager sessionManager = SessionManager.getInstance();
        assertNotNull(sessionManager, "SessionManager should be initialized");
        assertFalse(sessionManager.isAuthenticated(), "SessionManager should start unauthenticated");
    }
    
    @Test
    void testSingletonsSameInstance() {
        // Verify singletons return the same instance
        RestClient restClient1 = RestClient.getInstance();
        RestClient restClient2 = RestClient.getInstance();
        assertSame(restClient1, restClient2, "RestClient should return same instance");
        
        TokenManager tokenManager1 = TokenManager.getInstance();
        TokenManager tokenManager2 = TokenManager.getInstance();
        assertSame(tokenManager1, tokenManager2, "TokenManager should return same instance");
        
        SessionManager sessionManager1 = SessionManager.getInstance();
        SessionManager sessionManager2 = SessionManager.getInstance();
        assertSame(sessionManager1, sessionManager2, "SessionManager should return same instance");
    }
}
