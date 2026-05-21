package com.bachld.client;

import com.bachld.config.RestClient;
import com.bachld.exception.RestClientException;
import com.bachld.model.request.ChangePasswordRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class UserApiClient {

    private static final Logger logger = LoggerFactory.getLogger(UserApiClient.class);
    private final RestClient restClient;

    private static final String CHANGE_PASSWORD_ENDPOINT = "/api/user/v1/change-password";

    public UserApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void changePassword(ChangePasswordRequest request) throws RestClientException {
        try {
            RestTemplate restTemplate = restClient.getRestTemplate();
            String url = restClient.getBaseUrl() + CHANGE_PASSWORD_ENDPOINT;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ChangePasswordRequest> entity = new HttpEntity<>(request, headers);

            logger.debug("Sending PUT request to: {}", url);

            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);

            logger.info("Change password successful");

        } catch (HttpClientErrorException e) {
            String message = e.getStatusText();
            if (message == null || message.isBlank()) {
                message = "Đã xảy ra lỗi. Vui lòng thử lại.";
            }
            logger.warn("Change password failed - status: {}, message: {}", e.getStatusCode().value(), message);
            throw new RestClientException(message, e.getStatusCode().value(), e.getResponseBodyAsString(), e);

        } catch (Exception e) {
            logger.error("Change password request failed: {}", e.getMessage(), e);
            throw new RestClientException("Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.", e);
        }
    }
}