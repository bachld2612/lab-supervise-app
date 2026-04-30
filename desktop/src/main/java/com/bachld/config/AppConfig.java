package com.bachld.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static AppConfig instance;

    private final Properties properties;

    private AppConfig() {
        properties = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) {
                properties.load(is);
            } else {
                log.warn("application.properties not found on classpath. Using defaults.");
            }
        } catch (IOException e) {
            log.error("Failed to load application.properties", e);
        }
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    // ---- Getters ----

    public String getServerApiUrl() {
        return properties.getProperty("server.api.url", "http://localhost:8080/api");
    }

    public String getServerWsUrl() {
        return properties.getProperty("server.ws.url", "ws://localhost:8080/ws");
    }

    public int getMonitoringInterval() {
        return Integer.parseInt(
                properties.getProperty("monitoring.interval", "10"));
    }

    public String getAppVersion() {
        return properties.getProperty("app.version", "1.0.0");
    }

    public String getAppName() {
        return properties.getProperty("app.name", "TLU Lab Monitor");
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
