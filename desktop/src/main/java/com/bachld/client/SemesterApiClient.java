package com.bachld.client;

import com.bachld.config.RestClient;
import com.bachld.exception.RestClientException;
import com.bachld.model.response.SemesterPageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class SemesterApiClient {

    private static final Logger logger = LoggerFactory.getLogger(SemesterApiClient.class);
    private final RestClient restClient;

    public SemesterApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public SemesterPageResponse getSemesters() throws RestClientException {
        try {
            RestTemplate restTemplate = restClient.getRestTemplate();

            URI uri = UriComponentsBuilder
                    .fromHttpUrl(restClient.getBaseUrl() + "/api/semester/v1")
                    .queryParam("size", 1000)
                    .queryParam("sort", "startDate,desc")
                    .build()
                    .toUri();

            logger.debug("Fetching semesters from: {}", uri);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<SemesterPageResponse> responseEntity = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    requestEntity,
                    SemesterPageResponse.class
            );

            return responseEntity.getBody();

        } catch (Exception e) {
            logger.error("GET semesters request failed: {}", e.getMessage(), e);
            throw new RestClientException("Failed to fetch semesters: " + e.getMessage(), e);
        }
    }
}
