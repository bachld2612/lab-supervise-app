package com.bachld.client;

import com.bachld.config.RestClient;
import com.bachld.exception.RestClientException;
import com.bachld.model.request.PersonalComputerUpdateRequest;
import com.bachld.model.response.PersonalComputerErrorResponse;
import com.bachld.model.response.PersonalComputerResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for personal computer API endpoints.
 * Handles GET /me and POST /update requests.
 */
public class PersonalComputerApiClient {

    private static final Logger logger = LoggerFactory.getLogger(PersonalComputerApiClient.class);
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private static final String ME_ENDPOINT = "/api/personal-computer/v1/me";
    private static final String UPDATE_ENDPOINT = "/api/personal-computer/v1/update";

    public PersonalComputerApiClient(RestClient restClient) {
        this.restClient = restClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches the current user's personal computer info.
     * GET /api/personal-computer/v1/me
     *
     * @return PersonalComputerResponse with data (possibly null)
     * @throws RestClientException if something goes wrong
     */
    public PersonalComputerResponse getMyComputer() throws RestClientException {
        try {
            RestTemplate restTemplate = restClient.getRestTemplate();
            String url = restClient.getBaseUrl() + ME_ENDPOINT;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            logger.debug("Sending GET request to: {}", url);

            ResponseEntity<PersonalComputerResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    PersonalComputerResponse.class
            );

            logger.info("GET /me response status: {}", responseEntity.getStatusCode().value());
            return responseEntity.getBody();

        } catch (Exception e) {
            logger.error("GET /me request failed: {}", e.getMessage(), e);
            throw new RestClientException("Failed to fetch personal computer info: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the current user's personal computer IP address.
     * POST /api/personal-computer/v1/update
     *
     * On 400 error, parses the error response to extract field validation messages.
     *
     * @param request the update request containing ipAddress
     * @return PersonalComputerResponse on success
     * @throws RestClientException with error message on failure
     */
    public PersonalComputerResponse updateComputer(PersonalComputerUpdateRequest request)
            throws RestClientException {
        try {
            RestTemplate restTemplate = restClient.getRestTemplate();
            String url = restClient.getBaseUrl() + UPDATE_ENDPOINT;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PersonalComputerUpdateRequest> requestEntity = new HttpEntity<>(request, headers);

            logger.debug("Sending POST request to: {}", url);

            ResponseEntity<PersonalComputerResponse> responseEntity = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    PersonalComputerResponse.class
            );

            logger.info("POST /update response status: {}", responseEntity.getStatusCode().value());
            return responseEntity.getBody();

        } catch (HttpClientErrorException e) {
            // Handle 400 Bad Request - parse error response
            if (e.getStatusCode().value() == 400) {
                try {
                    String responseBody = e.getResponseBodyAsString();
                    PersonalComputerErrorResponse errorResponse =
                            objectMapper.readValue(responseBody, PersonalComputerErrorResponse.class);
                    String errorMsg = errorResponse.getFirstErrorMessage();
                    logger.warn("POST /update validation error: {}", errorMsg);
                    throw new RestClientException(errorMsg, 400, responseBody);
                } catch (RestClientException rce) {
                    throw rce;
                } catch (Exception parseEx) {
                    logger.error("Failed to parse 400 error response", parseEx);
                    throw new RestClientException("Dữ liệu không hợp lệ.", 400, e.getResponseBodyAsString());
                }
            }
            logger.error("POST /update HTTP error: {}", e.getStatusCode().value(), e);
            throw new RestClientException(
                    "Lỗi từ máy chủ: " + e.getStatusCode().value(),
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString(),
                    e
            );
        } catch (Exception e) {
            logger.error("POST /update request failed: {}", e.getMessage(), e);
            throw new RestClientException("Failed to update personal computer: " + e.getMessage(), e);
        }
    }
}
