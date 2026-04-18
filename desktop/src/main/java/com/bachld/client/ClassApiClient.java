package com.bachld.client;

import com.bachld.config.RestClient;
import com.bachld.exception.RestClientException;
import com.bachld.model.response.ClassListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

/**
 * Client for class-related API endpoints.
 */
public class ClassApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ClassApiClient.class);
    private final RestClient restClient;
    private static final String STUDENT_CLASSES_ENDPOINT = "/api/class/v1/student";

    public ClassApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Fetches the list of classes for the current student.
     * GET /api/class/v1/student
     */
    public ClassListResponse getMyClasses() throws RestClientException {
        try {
            RestTemplate restTemplate = restClient.getRestTemplate();
            String url = restClient.getBaseUrl() + STUDENT_CLASSES_ENDPOINT;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            logger.debug("Sending GET request to: {}", url);

            ResponseEntity<ClassListResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    ClassListResponse.class
            );

            logger.info("GET /student response status: {}", responseEntity.getStatusCode().value());
            return responseEntity.getBody();

        } catch (Exception e) {
            logger.error("GET /student request failed: {}", e.getMessage(), e);
            throw new RestClientException("Failed to fetch student classes: " + e.getMessage(), e);
        }
    }
}
