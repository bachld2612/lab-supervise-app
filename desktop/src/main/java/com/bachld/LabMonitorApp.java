package com.bachld;

import com.bachld.client.AuthApiClient;
import com.bachld.config.AppConfig;
import com.bachld.config.RestClient;
import com.bachld.service.AuthService;
import com.bachld.service.SessionManager;
import com.bachld.service.TokenManager;
import com.bachld.ui.LoginFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * LabMonitorApp - Main application entry point
 * Initializes all singletons and launches the login UI
 * 
 * Requirements: 7.4, 8.1, 9.1
 */
public class LabMonitorApp {

    private static final Logger log = LoggerFactory.getLogger(LabMonitorApp.class);

    public static void main(String[] args) {
        AppConfig config = AppConfig.getInstance();
        log.info("Starting {} v{}", config.getAppName(), config.getAppVersion());
        log.info("Server API: {}", config.getServerApiUrl());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.warn("Could not set system look and feel, using default.", e);
        }

        // Initialize singletons (Requirements 7.4, 8.1, 9.1)
        RestClient restClient = RestClient.getInstance();
        TokenManager tokenManager = TokenManager.getInstance();
        SessionManager sessionManager = SessionManager.getInstance();
        
        log.info("Initialized RestClient, TokenManager, and SessionManager");
        
        // Create AuthApiClient with RestClient
        AuthApiClient authApiClient = new AuthApiClient(restClient);
        log.info("Created AuthApiClient");
        
        // Create AuthService with dependencies
        AuthService authService = new AuthService(authApiClient, tokenManager, sessionManager);
        log.info("Created AuthService");

        // Display LoginFrame on EDT using SwingUtilities.invokeLater()
        SwingUtilities.invokeLater(() -> {
            log.info("Launching Login screen...");
            new LoginFrame(authService);
        });
    }
}
