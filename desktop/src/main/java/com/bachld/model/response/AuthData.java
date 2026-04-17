package com.bachld.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Authentication data model containing JWT token, user, and role information.
 * Nested within AuthResponse.
 * 
 * Validates: Requirements 7.2, 7.5, 7.6
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthData {
    
    @JsonProperty("token")
    private String token;
    
    @JsonProperty("user")
    private User user;
    
    @JsonProperty("role")
    private Role role;
    
    public AuthData() {
    }
    
    public AuthData(String token, User user, Role role) {
        this.token = token;
        this.user = user;
        this.role = role;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
}
