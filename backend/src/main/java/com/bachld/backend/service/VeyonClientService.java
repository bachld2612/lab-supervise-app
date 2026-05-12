package com.bachld.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VeyonClientService {

    @NonFinal
    @Value("${veyon.api.login}")
    String loginUrl;

    @NonFinal
    @Value("${veyon.api.lock-screen}")
    String lockScreenUrl;

    @NonFinal
    @Value("${veyon.api.screenshot}")
    String screenshotUrl;

    @NonFinal
    @Value("${veyon.api.open-website}")
    String openWebsiteUrl;

    @NonFinal
    @Value("${veyon.api.text-message}")
    String textMessageUrl;

    @NonFinal
    @Value("${veyon.screenshot.max-retries}")
    int screenshotMaxRetries;

    @NonFinal
    @Value("${veyon.screenshot.retry-delay-ms}")
    int screenshotRetryDelayMs;

    @NonFinal
    @Value("${veyon.screenshot.min-valid-size-bytes}")
    int minValidScreenshotSizeBytes;

    @NonFinal
    RestTemplate restTemplate;

    @NonFinal
    RestTemplate screenshotRestTemplate;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        restTemplate = new RestTemplate(factory);

        SimpleClientHttpRequestFactory screenshotFactory = new SimpleClientHttpRequestFactory();
        screenshotFactory.setConnectTimeout(5000);
        screenshotFactory.setReadTimeout(30000);
        screenshotRestTemplate = new RestTemplate(screenshotFactory);
    }

    /**
     * Calls Veyon auth API on the teacher's PC (teacherIp) to authenticate
     * against the student's PC (studentIp). Returns the connection-uid valid for 30s.
     */
    public String getConnectionUid(String keyName, String privateKeyContent, String teacherIp, String studentIp) {
        String url = loginUrl
                .replace("{{teacher_ip}}", teacherIp)
                .replace("{{student_ip}}", studentIp);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "method", "0c69b301-81b4-42d6-8fae-128cdd113314",
                "credentials", Map.of("keyname", keyName, "keydata", privateKeyContent)
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        Map<?, ?> responseBody = response.getBody();

        if (responseBody == null || responseBody.get("connection-uid") == null) {
            throw new RuntimeException("Không thể xác thực với Veyon: không nhận được connection-uid");
        }

        return responseBody.get("connection-uid").toString();
    }

    public void openWebsite(String connectionUid, String websiteUrl, String teacherIp) {
        String url = openWebsiteUrl.replace("{{teacher_ip}}", teacherIp);

        HttpHeaders headers = new HttpHeaders();
        headers.set("connection-uid", connectionUid);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("active", true, "arguments", Map.of("websiteUrls", List.of(websiteUrl)));
        restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
    }

    public void sendMessage(String connectionUid, String text, String teacherIp) {
        String url = textMessageUrl.replace("{{teacher_ip}}", teacherIp);

        HttpHeaders headers = new HttpHeaders();
        headers.set("connection-uid", connectionUid);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("active", true, "arguments", Map.of("text", text));
        restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
    }

    public void lockScreen(String connectionUid, boolean active, String teacherIp) {
        String url = lockScreenUrl.replace("{{teacher_ip}}", teacherIp);

        HttpHeaders headers = new HttpHeaders();
        headers.set("connection-uid", connectionUid);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("active", active, "arguments", Map.of());
        restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
    }

    public byte[] getScreenshot(String connectionUid, String teacherIp) {
        String url = screenshotUrl.replace("{{teacher_ip}}", teacherIp);

        HttpHeaders headers = new HttpHeaders();
        headers.set("connection-uid", connectionUid);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        for (int attempt = 1; attempt <= screenshotMaxRetries; attempt++) {
            boolean needRetry = false;
            try {
                ResponseEntity<byte[]> response = screenshotRestTemplate.exchange(
                        url, HttpMethod.GET, entity, byte[].class
                );
                byte[] body = response.getBody();
                if (body == null) {
                    throw new RuntimeException("Không nhận được dữ liệu ảnh từ Veyon");
                }
                // Ảnh hợp lệ — trả về ngay
                if (body.length >= minValidScreenshotSizeBytes) {
                    return body;
                }
                // Veyon trả 200 OK nhưng ảnh quá nhỏ = black frame (VNC chưa capture kịp màn hình)
                needRetry = true;
            } catch (HttpServerErrorException.ServiceUnavailable e) {
                // Framebuffer chưa sẵn sàng — Veyon cần thêm thời gian sau khi auth
                needRetry = true;
            }

            if (needRetry) {
                if (attempt == screenshotMaxRetries) {
                    throw new RuntimeException(
                            "Veyon framebuffer chưa sẵn sàng sau " + screenshotMaxRetries + " lần thử"
                    );
                }
                try {
                    Thread.sleep(screenshotRetryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Bị ngắt khi chờ Veyon framebuffer");
                }
            }
        }

        throw new RuntimeException("Không nhận được dữ liệu ảnh từ Veyon");
    }
}
