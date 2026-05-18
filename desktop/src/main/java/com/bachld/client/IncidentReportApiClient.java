package com.bachld.client;

import com.bachld.config.RestClient;
import com.bachld.exception.RestClientException;
import com.bachld.model.request.IncidentReportCreateRequest;
import com.bachld.model.response.IncidentReportListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class IncidentReportApiClient {

    private static final Logger logger = LoggerFactory.getLogger(IncidentReportApiClient.class);
    private final RestClient restClient;

    private static final String STUDENT_ENDPOINT = "/api/incident-report/v1/student";

    public IncidentReportApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public IncidentReportListResponse getMyReports(int page, int size) throws RestClientException {
        try {
            RestTemplate restTemplate = restClient.getRestTemplate();
            String url = restClient.getBaseUrl() + STUDENT_ENDPOINT + "?page=" + page + "&size=" + size;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            logger.debug("GET incident reports: {}", url);
            ResponseEntity<IncidentReportListResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, IncidentReportListResponse.class);

            return response.getBody();
        } catch (HttpClientErrorException e) {
            logger.warn("GET reports HTTP error: {}", e.getStatusCode().value());
            throw new RestClientException(e.getStatusText(), e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.error("GET reports failed", e);
            throw new RestClientException("Lỗi kết nối đến máy chủ.", e);
        }
    }

    public void createReport(IncidentReportCreateRequest request) throws RestClientException {
        try {
            RestTemplate restTemplate = restClient.getRestTemplate();
            String url = restClient.getBaseUrl() + STUDENT_ENDPOINT;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<IncidentReportCreateRequest> entity = new HttpEntity<>(request, headers);

            logger.debug("POST incident report");
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (HttpClientErrorException e) {
            logger.warn("POST report HTTP error: {}", e.getStatusCode().value());
            throw new RestClientException(e.getStatusText(), e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.error("POST report failed", e);
            throw new RestClientException("Lỗi kết nối đến máy chủ.", e);
        }
    }

    public void updateReport(int id, IncidentReportCreateRequest request) throws RestClientException {
        try {
            RestTemplate restTemplate = restClient.getRestTemplate();
            String url = restClient.getBaseUrl() + STUDENT_ENDPOINT + "/" + id;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<IncidentReportCreateRequest> entity = new HttpEntity<>(request, headers);

            logger.debug("PUT incident report {}", id);
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
        } catch (HttpClientErrorException e) {
            logger.warn("PUT report {} HTTP error: {}", id, e.getStatusCode().value());
            throw new RestClientException(e.getStatusText(), e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.error("PUT report {} failed", id, e);
            throw new RestClientException("Lỗi kết nối đến máy chủ.", e);
        }
    }
}