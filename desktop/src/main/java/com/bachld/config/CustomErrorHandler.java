package com.bachld.config;

import com.bachld.model.response.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * CustomErrorHandler extends DefaultResponseErrorHandler to extract error messages
 * from HTTP error responses.
 * 
 * This handler specifically processes 422 (Unprocessable Entity) status codes and
 * extracts the error message from the response body's message field. For other error
 * status codes, it delegates to the default error handling behavior.
 * 
 * Validates: Requirements 6.4, 7.6, 11.3
 */
public class CustomErrorHandler extends DefaultResponseErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomErrorHandler.class);
    private final ObjectMapper objectMapper;
    
    /**
     * Constructs a CustomErrorHandler with a default ObjectMapper.
     */
    public CustomErrorHandler() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Constructs a CustomErrorHandler with the specified ObjectMapper.
     * 
     * @param objectMapper the ObjectMapper to use for JSON deserialization
     */
    public CustomErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handles HTTP error responses by extracting error messages from the response body.
     * 
     * For 422 status codes, this method extracts the error message from the response
     * body's message field and throws an HttpClientErrorException with that message.
     * For other error status codes, it delegates to the default error handling.
     * 
     * @param response the HTTP response with an error status code
     * @throws IOException if an I/O error occurs while reading the response
     */
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = HttpStatus.resolve(response.getStatusCode().value());
        
        logger.error("HTTP error response - Status: {}", statusCode);
        
        // Handle 422 Unprocessable Entity specifically
        if (statusCode == HttpStatus.UNPROCESSABLE_ENTITY) {
            String responseBody = new String(
                response.getBody().readAllBytes(), 
                StandardCharsets.UTF_8
            );
            
            try {
                ApiErrorResponse errorResponse = objectMapper.readValue(
                    responseBody, 
                    ApiErrorResponse.class
                );
                
                // Extract the message field and throw exception with it
                String errorMessage = errorResponse.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = statusCode.getReasonPhrase();
                }
                
                logger.error("API error - Status: 422, Message: {}", errorMessage);
                
                throw HttpClientErrorException.create(
                    statusCode,
                    errorMessage,
                    response.getHeaders(),
                    responseBody.getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            } catch (HttpClientErrorException e) {
                // Re-throw if it's already our custom exception
                throw e;
            } catch (Exception e) {
                // If JSON parsing fails, fall back to default error handling
                logger.error("Failed to parse error response", e);
                super.handleError(response);
            }
        } else {
            // For all other error status codes, use default handling
            try {
                super.handleError(response);
            } catch (Exception e) {
                logger.error("Error handling HTTP response - Status: {}", statusCode, e);
                throw e;
            }
        }
    }
}
