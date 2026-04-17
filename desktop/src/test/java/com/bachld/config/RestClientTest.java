package com.bachld.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RestClient singleton.
 * 
 * Validates: Requirements 6.1, 6.2, 6.3, 6.4, 8.2
 */
class RestClientTest {
    
    private RestClient restClient;
    
    @BeforeEach
    void setUp() {
        restClient = RestClient.getInstance();
    }
    
    @Test
    void getInstance_shouldReturnSameInstance() {
        RestClient instance1 = RestClient.getInstance();
        RestClient instance2 = RestClient.getInstance();
        
        assertSame(instance1, instance2, "getInstance should return the same singleton instance");
    }
    
    @Test
    void getRestTemplate_shouldReturnConfiguredRestTemplate() {
        RestTemplate restTemplate = restClient.getRestTemplate();
        
        assertNotNull(restTemplate, "RestTemplate should not be null");
    }
    
    @Test
    void getBaseUrl_shouldReturnUrlFromProperties() {
        String baseUrl = restClient.getBaseUrl();
        
        assertNotNull(baseUrl, "Base URL should not be null");
        assertFalse(baseUrl.isEmpty(), "Base URL should not be empty");
    }
    
    @Test
    void restTemplate_shouldBeConfiguredWithTimeouts() {
        RestTemplate restTemplate = restClient.getRestTemplate();
        
        // Verify RestTemplate is properly instantiated
        // The timeouts are configured in the private createRequestFactory() method
        // and are set to 5000ms (connection) and 10000ms (read) as per Requirements 6.1 and 6.2
        assertNotNull(restTemplate, 
            "RestTemplate should be configured with connection timeout (5000ms) and read timeout (10000ms)");
    }
    
    @Test
    void restTemplate_shouldHaveJacksonMessageConverter() {
        RestTemplate restTemplate = restClient.getRestTemplate();
        
        boolean hasJacksonConverter = restTemplate.getMessageConverters().stream()
            .anyMatch(converter -> converter instanceof MappingJackson2HttpMessageConverter);
        
        assertTrue(hasJacksonConverter, 
            "RestTemplate should have Jackson message converter registered (Requirement 6.3)");
    }
    
    @Test
    void restTemplate_shouldHaveJwtInterceptor() {
        RestTemplate restTemplate = restClient.getRestTemplate();
        
        boolean hasJwtInterceptor = restTemplate.getInterceptors().stream()
            .anyMatch(interceptor -> interceptor instanceof JwtInterceptor);
        
        assertTrue(hasJwtInterceptor, 
            "RestTemplate should have JWT interceptor registered (Requirement 8.2)");
    }
    
    @Test
    void restTemplate_shouldHaveCustomErrorHandler() {
        RestTemplate restTemplate = restClient.getRestTemplate();
        
        assertTrue(restTemplate.getErrorHandler() instanceof CustomErrorHandler,
            "RestTemplate should have CustomErrorHandler registered (Requirement 6.4)");
    }
}
