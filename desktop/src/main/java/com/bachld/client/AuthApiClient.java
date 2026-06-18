package com.bachld.client;

import com.bachld.config.RestClient;
import com.bachld.exception.RestClientException;
import com.bachld.model.response.AuthResponse;
import com.bachld.model.request.LoginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Client for authentication API endpoints.
 * Handles login requests and authentication-related API calls.
 * 
 * Requirements: 7.1, 7.2, 7.3, 11.2
 */
public class AuthApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthApiClient.class);
    private final RestClient restClient;
    private static final String LOGIN_ENDPOINT = "/api/auth/v1/login";
    private static final String REFRESH_ENDPOINT = "/api/auth/v1/refresh";
    private static final String LOGOUT_ENDPOINT = "/api/auth/v1/logout";
    
    /**
     * Constructs an AuthApiClient with the specified RestClient.
     * 
     * @param restClient the RestClient instance to use for API calls
     */
    public AuthApiClient(RestClient restClient) {
        this.restClient = restClient;
    }
    
    /**
     * Performs login authentication by sending credentials to the server.
     * 
     * This method POSTs to /api/auth/v1/login endpoint with email, password,
     * and device in the request body as JSON with Content-Type header
     * set to application/json.
     * 
     * @param email the user's email address
     * @param password the user's password
     * @param device the device type (e.g., "desktop")
     * @return AuthResponse containing authentication data on success
     * @throws RestClientException if the API call fails or returns an error
     */
    public AuthResponse login(String email, String password, String device, String wifiSsid)
            throws RestClientException {
        try {
            LoginRequest loginRequest = new LoginRequest(email, password, device, wifiSsid);
            
            // Set Content-Type header to application/json
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity with headers and body
            HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginRequest, headers);
            
            // Get RestTemplate and base URL
            RestTemplate restTemplate = restClient.getRestTemplate();
            String url = restClient.getBaseUrl() + LOGIN_ENDPOINT;
            
            logger.debug("Sending POST request to: {}", url);
            
            // Execute POST request
            ResponseEntity<AuthResponse> responseEntity = restTemplate.postForEntity(
                url,
                requestEntity,
                AuthResponse.class
            );
            
            // Log response status
            int statusCode = responseEntity.getStatusCode().value();
            logger.info("API response status: {}", statusCode);
            
            // Return response body
            return responseEntity.getBody();
            
        } catch (Exception e) {
            logger.error("API request failed: {}", e.getMessage(), e);
            throw new RestClientException(
                "Failed to authenticate: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Exchanges the refresh-token cookie (sent automatically by the cookie
     * manager) for a new access token. Returns null if refresh fails.
     */
    public AuthResponse refresh() {
        try {
            RestTemplate restTemplate = restClient.getRestTemplate();
            String url = restClient.getBaseUrl() + REFRESH_ENDPOINT;
            ResponseEntity<AuthResponse> responseEntity =
                    restTemplate.postForEntity(url, null, AuthResponse.class);
            return responseEntity.getBody();
        } catch (Exception e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Logs out server-side: deletes the refresh token from DB and blacklists the
     * current access token. Best-effort — failures are swallowed.
     */
    public void logout() {
        try {
            RestTemplate restTemplate = restClient.getRestTemplate();
            String url = restClient.getBaseUrl() + LOGOUT_ENDPOINT;
            restTemplate.postForEntity(url, null, Void.class);
        } catch (Exception e) {
            logger.warn("Logout request failed: {}", e.getMessage());
        }
    }
}
