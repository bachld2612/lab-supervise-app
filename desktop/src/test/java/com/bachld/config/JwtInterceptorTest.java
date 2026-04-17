package com.bachld.config;

import com.bachld.service.TokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtInterceptor.
 * 
 * Tests verify that the interceptor correctly injects Authorization headers
 * when tokens are present and skips injection when no token is stored.
 */
class JwtInterceptorTest {
    
    private TokenManager tokenManager;
    private JwtInterceptor interceptor;
    private TestHttpRequest request;
    private ClientHttpRequestExecution execution;
    private byte[] body;
    
    @BeforeEach
    void setUp() {
        tokenManager = TokenManager.getInstance();
        tokenManager.clearToken(); // Ensure clean state
        interceptor = new JwtInterceptor(tokenManager);
        request = new TestHttpRequest();
        body = new byte[0];
        
        // Mock execution that returns a dummy response
        execution = (req, b) -> {
            // Return a mock response (not used in these tests)
            return null;
        };
    }
    
    /**
     * Simple test implementation of HttpRequest for testing purposes.
     */
    private static class TestHttpRequest implements HttpRequest {
        private final HttpHeaders headers = new HttpHeaders();
        private final URI uri = URI.create("http://localhost:8080/api/test");
        
        @Override
        public HttpMethod getMethod() {
            return HttpMethod.GET;
        }
        
        @Override
        public URI getURI() {
            return uri;
        }
        
        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
    
    @Test
    void intercept_shouldAddAuthorizationHeader_whenTokenExists() throws IOException {
        // Given: A token is stored
        String testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        tokenManager.setToken(testToken);
        
        // When: Interceptor processes the request
        interceptor.intercept(request, body, execution);
        
        // Then: Authorization header should be added with Bearer format
        HttpHeaders headers = request.getHeaders();
        assertTrue(headers.containsKey("Authorization"), 
                  "Authorization header should be present");
        assertEquals("Bearer " + testToken, headers.getFirst("Authorization"),
                    "Authorization header should have Bearer format with token");
    }
    
    @Test
    void intercept_shouldNotAddAuthorizationHeader_whenNoTokenExists() throws IOException {
        // Given: No token is stored
        tokenManager.clearToken();
        
        // When: Interceptor processes the request
        interceptor.intercept(request, body, execution);
        
        // Then: Authorization header should not be added
        HttpHeaders headers = request.getHeaders();
        assertFalse(headers.containsKey("Authorization"),
                   "Authorization header should not be present when no token exists");
    }
    
    @Test
    void intercept_shouldNotAddAuthorizationHeader_whenTokenIsEmpty() throws IOException {
        // Given: An empty token is stored
        tokenManager.setToken("");
        
        // When: Interceptor processes the request
        interceptor.intercept(request, body, execution);
        
        // Then: Authorization header should not be added
        HttpHeaders headers = request.getHeaders();
        assertFalse(headers.containsKey("Authorization"),
                   "Authorization header should not be present when token is empty");
    }
    
    @Test
    void intercept_shouldOverwriteExistingAuthorizationHeader_whenTokenExists() throws IOException {
        // Given: Request already has an Authorization header and a token is stored
        request.getHeaders().set("Authorization", "Bearer old-token");
        String newToken = "new-jwt-token";
        tokenManager.setToken(newToken);
        
        // When: Interceptor processes the request
        interceptor.intercept(request, body, execution);
        
        // Then: Authorization header should be overwritten with new token
        HttpHeaders headers = request.getHeaders();
        assertEquals("Bearer " + newToken, headers.getFirst("Authorization"),
                    "Authorization header should be overwritten with new token");
    }
    
    @Test
    void intercept_shouldCallExecution_whenTokenExists() throws IOException {
        // Given: A token is stored and execution is tracked
        tokenManager.setToken("test-token");
        final boolean[] executionCalled = {false};
        ClientHttpRequestExecution trackingExecution = (req, b) -> {
            executionCalled[0] = true;
            return null;
        };
        
        // When: Interceptor processes the request
        interceptor.intercept(request, body, trackingExecution);
        
        // Then: Execution should be called
        assertTrue(executionCalled[0], "Execution should be called");
    }
    
    @Test
    void intercept_shouldCallExecution_whenNoTokenExists() throws IOException {
        // Given: No token is stored and execution is tracked
        tokenManager.clearToken();
        final boolean[] executionCalled = {false};
        ClientHttpRequestExecution trackingExecution = (req, b) -> {
            executionCalled[0] = true;
            return null;
        };
        
        // When: Interceptor processes the request
        interceptor.intercept(request, body, trackingExecution);
        
        // Then: Execution should still be called
        assertTrue(executionCalled[0], "Execution should be called even without token");
    }
}
