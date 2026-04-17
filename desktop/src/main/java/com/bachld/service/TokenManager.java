package com.bachld.service;

/**
 * TokenManager singleton for managing JWT authentication tokens in memory.
 * 
 * This class provides thread-safe access to store and retrieve JWT tokens
 * used for authenticated API requests. The token is stored in memory only
 * and is not persisted to disk.
 * 
 * Requirements: 8.1, 8.3
 */
public class TokenManager {
    
    private static volatile TokenManager instance;
    private volatile String jwtToken;
    
    /**
     * Private constructor to prevent direct instantiation.
     */
    private TokenManager() {
    }
    
    /**
     * Returns the singleton instance of TokenManager using double-checked locking
     * for thread-safe lazy initialization.
     * 
     * @return the singleton TokenManager instance
     */
    public static TokenManager getInstance() {
        if (instance == null) {
            synchronized (TokenManager.class) {
                if (instance == null) {
                    instance = new TokenManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Stores the JWT token in memory.
     * 
     * @param token the JWT token to store
     */
    public synchronized void setToken(String token) {
        this.jwtToken = token;
    }
    
    /**
     * Retrieves the stored JWT token.
     * 
     * @return the JWT token, or null if no token is stored
     */
    public synchronized String getToken() {
        return jwtToken;
    }
    
    /**
     * Checks if a token is currently stored.
     * 
     * @return true if a token is stored, false otherwise
     */
    public synchronized boolean hasToken() {
        return jwtToken != null && !jwtToken.isEmpty();
    }
    
    /**
     * Clears the stored JWT token.
     */
    public synchronized void clearToken() {
        this.jwtToken = null;
    }
}
