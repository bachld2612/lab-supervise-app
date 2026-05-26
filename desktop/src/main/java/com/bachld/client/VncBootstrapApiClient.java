package com.bachld.client;

import com.bachld.config.RestClient;
import com.bachld.exception.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Đăng ký VNC password (do installer sinh ra) lên backend ngay sau khi user login lần đầu.
 * Backend lưu encrypted vào DB và dùng password đó khi giảng viên tạo session relay.
 */
public class VncBootstrapApiClient {

    private static final Logger log = LoggerFactory.getLogger(VncBootstrapApiClient.class);

    private static final String REGISTER_ENDPOINT = "/api/vnc/v1/register";

    private final RestClient restClient;

    public VncBootstrapApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /** Gửi password plaintext lên backend (TLS sẽ encrypt trên đường truyền). */
    public void registerPassword(String vncPassword) throws RestClientException {
        try {
            RestTemplate restTemplate = restClient.getRestTemplate();
            String url = restClient.getBaseUrl() + REGISTER_ENDPOINT;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(Map.of("vncPassword", vncPassword), headers);

            ResponseEntity<Void> response = restTemplate.postForEntity(url, requestEntity, Void.class);
            log.info("POST {} → {}", REGISTER_ENDPOINT, response.getStatusCode().value());
        } catch (Exception e) {
            log.error("VNC bootstrap registration failed: {}", e.getMessage(), e);
            throw new RestClientException("Failed to register VNC password: " + e.getMessage(), e);
        }
    }
}
