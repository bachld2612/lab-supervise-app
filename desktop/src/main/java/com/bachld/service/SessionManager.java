package com.bachld.service;

import com.bachld.model.response.Role;
import com.bachld.model.response.User;

/**
 * SessionManager singleton for managing user session information.
 * Stores authenticated user and role information in memory.
 * 
 * Validates: Requirements 9.1, 9.2, 9.3, 9.4
 */
public class SessionManager {
    
    private static SessionManager instance;
    private User currentUser;
    private Role currentRole;
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private SessionManager() {
    }
    
    /**
     * Thread-safe singleton instance retrieval.
     * 
     * @return the singleton SessionManager instance
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Sets the current user session with user and role information.
     * 
     * @param user the authenticated user
     * @param role the user's role
     */
    public void setSession(User user, Role role) {
        this.currentUser = user;
        this.currentRole = role;
    }
    
    /**
     * Retrieves the current authenticated user.
     * 
     * @return the current user, or null if not authenticated
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Retrieves the current user's role.
     * 
     * @return the current role, or null if not authenticated
     */
    public Role getCurrentRole() {
        return currentRole;
    }
    
    /**
     * Checks if a user is currently authenticated.
     * 
     * @return true if user and role are set, false otherwise
     */
    public boolean isAuthenticated() {
        return currentUser != null && currentRole != null;
    }
    
    /**
     * Clears the current session, removing user and role information.
     */
    public void clearSession() {
        this.currentUser = null;
        this.currentRole = null;
    }
}
