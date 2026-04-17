package com.bachld.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error response model for API error responses.
 * Used to deserialize error messages from failed API calls.
 * 
 * Validates: Requirements 7.2, 7.5, 7.6
 */
public class ApiErrorResponse {
    
    @JsonProperty("statusCode")
    private int statusCode;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("error")
    private String error;
    
    public ApiErrorResponse() {
    }
    
    public ApiErrorResponse(int statusCode, String message, String error) {
        this.statusCode = statusCode;
        this.message = message;
        this.error = error;
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
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
