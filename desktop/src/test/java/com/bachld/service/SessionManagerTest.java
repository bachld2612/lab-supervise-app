package com.bachld.service;

import com.bachld.model.response.Role;
import com.bachld.model.response.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SessionManager singleton.
 * 
 * Validates: Requirements 9.1, 9.2, 9.3, 9.4
 */
class SessionManagerTest {
    
    private SessionManager sessionManager;
    
    @BeforeEach
    void setUp() {
        sessionManager = SessionManager.getInstance();
        // Clear session before each test
        sessionManager.clearSession();
    }
    
    @Test
    void testGetInstanceReturnsSameInstance() {
        SessionManager instance1 = SessionManager.getInstance();
        SessionManager instance2 = SessionManager.getInstance();
        
        assertSame(instance1, instance2, "getInstance should return the same instance");
    }
    
    @Test
    void testSetSessionStoresUserAndRole() {
        User user = new User(1L, "test@example.com", "Test User", "SV001");
        Role role = new Role(1L, "STUDENT", "Student role");
        
        sessionManager.setSession(user, role);
        
        assertEquals(user, sessionManager.getCurrentUser());
        assertEquals(role, sessionManager.getCurrentRole());
    }
    
    @Test
    void testGetCurrentUserReturnsNullWhenNotAuthenticated() {
        assertNull(sessionManager.getCurrentUser());
    }
    
    @Test
    void testGetCurrentRoleReturnsNullWhenNotAuthenticated() {
        assertNull(sessionManager.getCurrentRole());
    }
    
    @Test
    void testIsAuthenticatedReturnsFalseWhenNoSession() {
        assertFalse(sessionManager.isAuthenticated());
    }
    
    @Test
    void testIsAuthenticatedReturnsTrueWhenSessionSet() {
        User user = new User(1L, "test@example.com", "Test User", "SV001");
        Role role = new Role(1L, "STUDENT", "Student role");
        
        sessionManager.setSession(user, role);
        
        assertTrue(sessionManager.isAuthenticated());
    }
    
    @Test
    void testIsAuthenticatedReturnsFalseWhenOnlyUserSet() {
        User user = new User(1L, "test@example.com", "Test User", "SV001");
        
        sessionManager.setSession(user, null);
        
        assertFalse(sessionManager.isAuthenticated());
    }
    
    @Test
    void testIsAuthenticatedReturnsFalseWhenOnlyRoleSet() {
        Role role = new Role(1L, "STUDENT", "Student role");
        
        sessionManager.setSession(null, role);
        
        assertFalse(sessionManager.isAuthenticated());
    }
    
    @Test
    void testClearSessionRemovesUserAndRole() {
        User user = new User(1L, "test@example.com", "Test User", "SV001");
        Role role = new Role(1L, "STUDENT", "Student role");
        
        sessionManager.setSession(user, role);
        sessionManager.clearSession();
        
        assertNull(sessionManager.getCurrentUser());
        assertNull(sessionManager.getCurrentRole());
        assertFalse(sessionManager.isAuthenticated());
    }
    
    @Test
    void testSetSessionOverwritesPreviousSession() {
        User user1 = new User(1L, "user1@example.com", "User One", "SV001");
        Role role1 = new Role(1L, "STUDENT", "Student role");
        
        User user2 = new User(2L, "user2@example.com", "User Two", "SV002");
        Role role2 = new Role(2L, "ADMIN", "Admin role");
        
        sessionManager.setSession(user1, role1);
        sessionManager.setSession(user2, role2);
        
        assertEquals(user2, sessionManager.getCurrentUser());
        assertEquals(role2, sessionManager.getCurrentRole());
    }
}
