package com.bachld.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for authentication API endpoint.
 * Contains status code, message, and authentication data.
 * 
 * Validates: Requirements 7.2, 7.5, 7.6
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResponse {
    
    @JsonProperty("statusCode")
    private int statusCode;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private AuthData data;
    
    public AuthResponse() {
    }
    
    public AuthResponse(int statusCode, String message, AuthData data) {
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public AuthData getData() {
        return data;
    }
    
    public void setData(AuthData data) {
        this.data = data;
    }
}
