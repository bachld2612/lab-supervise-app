package com.bachld.client;

import com.bachld.config.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fetches the list of valid WiFi SSIDs for a student from the public auth endpoint.
 * Called before login to determine which SSIDs are acceptable for location verification.
 */
public class WifiAuthApiClient {
    private static final Logger log = LoggerFactory.getLogger(WifiAuthApiClient.class);
    private static final String ENDPOINT = "/api/auth/v1/wifi-ssid";

    private final RestClient restClient;

    public WifiAuthApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Returns the list of valid WiFi SSIDs for the student's active classes today.
     * Returns an empty list on any error or if the student has no active classes with a configured SSID.
     */
    @SuppressWarnings("unchecked")
    public List<String> getValidSsids(String email) {
        try {
            String url = restClient.getBaseUrl() + ENDPOINT + "?email={email}";
            RestTemplate restTemplate = restClient.getRestTemplate();
            Map<?, ?> response = restTemplate.getForObject(url, Map.class, email);
            if (response != null && response.get("data") instanceof List) {
                log.error("------------------------------------------------------------");
                log.info(response.get("data").toString());
                log.error("------------------------------------------------------------");
                return (List<String>) response.get("data");
            }
        } catch (Exception e) {
            log.warn("Could not fetch valid WiFi SSIDs: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
}
