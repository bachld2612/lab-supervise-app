package com.bachld.config;

import com.bachld.service.TokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * JwtInterceptor automatically injects JWT authentication tokens into outgoing HTTP requests.
 * 
 * This interceptor implements the ClientHttpRequestInterceptor interface to intercept
 * all HTTP requests made by RestTemplate and add the Authorization header with the
 * JWT token when available.
 * 
 * Requirements: 8.2, 8.3, 8.4, 11.2
 */
public class JwtInterceptor implements ClientHttpRequestInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtInterceptor.class);
    private final TokenManager tokenManager;
    
    /**
     * Constructs a JwtInterceptor with the specified TokenManager.
     * 
     * @param tokenManager the TokenManager instance to retrieve JWT tokens from
     */
    public JwtInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }
    
    /**
     * Intercepts HTTP requests and injects the Authorization header with JWT token.
     * 
     * If a token is stored in TokenManager, this method adds an Authorization header
     * with the format "Bearer {token}". If no token is stored, the request proceeds
     * without modification.
     * 
     * @param request the HTTP request to intercept
     * @param body the request body
     * @param execution the request execution chain
     * @return the HTTP response
     * @throws IOException if an I/O error occurs during request execution
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, 
                                       ClientHttpRequestExecution execution) throws IOException {
        // Log request details
        logger.debug("Intercepting request: {} {}", request.getMethod(), request.getURI());
        
        // Only add Authorization header if a token exists
        if (tokenManager.hasToken()) {
            String token = tokenManager.getToken();
            request.getHeaders().set("Authorization", "Bearer " + token);
            logger.debug("Added Authorization header to request");
        }
        
        // Continue with the request execution
        ClientHttpResponse response = execution.execute(request, body);
        
        // Log response status if response is not null
        if (response != null) {
            logger.info("Request completed - Status: {}", response.getStatusCode().value());
        }
        
        return response;
    }
}
