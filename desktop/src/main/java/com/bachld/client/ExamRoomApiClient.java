package com.bachld.client;

import com.bachld.config.RestClient;
import com.bachld.exception.RestClientException;
import com.bachld.model.response.ExamRoomListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class ExamRoomApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ExamRoomApiClient.class);
    private final RestClient restClient;
    private static final String STUDENT_EXAM_ROOMS_ENDPOINT = "/api/exam-room/v1/student";

    public ExamRoomApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public ExamRoomListResponse getMyExamRooms() throws RestClientException {
        try {
            RestTemplate restTemplate = restClient.getRestTemplate();
            String url = restClient.getBaseUrl() + STUDENT_EXAM_ROOMS_ENDPOINT;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<ExamRoomListResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    ExamRoomListResponse.class
            );

            logger.info("GET /exam-room/v1/student response status: {}", responseEntity.getStatusCode().value());
            return responseEntity.getBody();

        } catch (Exception e) {
            logger.error("GET /exam-room/v1/student request failed: {}", e.getMessage(), e);
            throw new RestClientException("Failed to fetch student exam rooms: " + e.getMessage(), e);
        }
    }
}
