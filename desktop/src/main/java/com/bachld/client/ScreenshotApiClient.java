package com.bachld.client;

import com.bachld.config.RestClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class ScreenshotApiClient {

    private final RestClient restClient;

    public ScreenshotApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void uploadScreenshot(Integer screenshotId, byte[] imageBytes) {
        String url = restClient.getBaseUrl() + "/api/screenshots/v1/" + screenshotId + "/image";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return screenshotId + ".jpg";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", imageResource);

        restClient.getRestTemplate().postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }
}
