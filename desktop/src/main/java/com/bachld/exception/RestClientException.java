package com.bachld.exception;

/**
 * Exception thrown when REST API calls fail.
 * Wraps underlying HTTP client exceptions and provides error details.
 */
public class RestClientException extends Exception {
    
    private final int statusCode;
    private final String responseBody;
    
    public RestClientException(String message) {
        super(message);
        this.statusCode = -1;
        this.responseBody = null;
    }
    
    public RestClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }
    
    public RestClientException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
    
    public RestClientException(String message, int statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getResponseBody() {
        return responseBody;
    }
    
    public boolean hasStatusCode() {
        return statusCode > 0;
    }
}
