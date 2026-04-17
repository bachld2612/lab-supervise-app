package com.bachld.config;

import com.bachld.service.TokenManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * RestClient singleton provides a configured RestTemplate instance for API communication.
 * 
 * This class implements the singleton pattern with thread-safe initialization and configures
 * RestTemplate with appropriate timeouts, message converters, interceptors, and error handlers
 * for reliable API communication.
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 8.2
 */
public class RestClient {
    
    private static volatile RestClient instance;
    private final RestTemplate restTemplate;
    private final String baseUrl;
    
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    private static final int READ_TIMEOUT = 10000; // 10 seconds
    
    /**
     * Private constructor to prevent direct instantiation.
     * Initializes RestTemplate with all required configurations.
     */
    private RestClient() {
        this.baseUrl = AppConfig.getInstance().getServerApiUrl();
        this.restTemplate = createRestTemplate();
    }
    
    /**
     * Returns the singleton instance of RestClient using double-checked locking
     * for thread-safe lazy initialization.
     * 
     * @return the singleton RestClient instance
     */
    public static RestClient getInstance() {
        if (instance == null) {
            synchronized (RestClient.class) {
                if (instance == null) {
                    instance = new RestClient();
                }
            }
        }
        return instance;
    }
    
    /**
     * Returns the configured RestTemplate instance.
     * 
     * @return the RestTemplate configured with timeouts, converters, interceptors, and error handler
     */
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
    
    /**
     * Returns the base URL for API requests.
     * 
     * @return the base URL read from application.properties
     */
    public String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * Creates and configures a RestTemplate instance with all required settings.
     * 
     * Configuration includes:
     * - Connection timeout: 5000ms (Requirement 6.1)
     * - Read timeout: 10000ms (Requirement 6.2)
     * - Jackson message converter for JSON (Requirement 6.3)
     * - JWT interceptor for authentication (Requirement 8.2)
     * - Custom error handler (Requirement 6.4)
     * 
     * @return configured RestTemplate instance
     */
    private RestTemplate createRestTemplate() {
        RestTemplate template = new RestTemplate(createRequestFactory());
        
        // Register Jackson message converter for JSON serialization/deserialization
        ObjectMapper objectMapper = new ObjectMapper();
        MappingJackson2HttpMessageConverter messageConverter = 
            new MappingJackson2HttpMessageConverter(objectMapper);
        template.getMessageConverters().add(0, messageConverter);
        
        // Register JWT interceptor for automatic token injection
        TokenManager tokenManager = TokenManager.getInstance();
        JwtInterceptor jwtInterceptor = new JwtInterceptor(tokenManager);
        template.getInterceptors().add(jwtInterceptor);
        
        // Register custom error handler for HTTP error processing
        CustomErrorHandler errorHandler = new CustomErrorHandler(objectMapper);
        template.setErrorHandler(errorHandler);
        
        return template;
    }
    
    /**
     * Creates a ClientHttpRequestFactory with configured timeouts.
     * 
     * @return ClientHttpRequestFactory with connection and read timeouts set
     */
    private ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECTION_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }
}
