package com.bachld;

import com.bachld.client.AuthApiClient;
import com.bachld.config.AppConfig;
import com.bachld.config.RestClient;
import com.bachld.service.AuthService;
import com.bachld.service.SessionManager;
import com.bachld.service.TokenManager;
import com.bachld.service.vnc.AdminPrivilegeService;
import com.bachld.service.vnc.VncBootstrapService;
import com.bachld.ui.LoginFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class LabMonitorApp {

    private static final Logger log = LoggerFactory.getLogger(LabMonitorApp.class);

    public static void main(String[] args) {
        String os = System.getProperty("os.name", "").toLowerCase();

        // On Windows: require Administrator, then bootstrap UltraVNC before anything else
        if (os.contains("win")) {
            AdminPrivilegeService adminService = new AdminPrivilegeService();
            if (!adminService.isRunningAsAdmin()) {
                adminService.relaunchAsAdminAndExit();
                return;
            }

            Thread vncThread = new Thread(() -> {
                try {
                    new VncBootstrapService().ensureReady();
                } catch (Exception e) {
                    log.error("VNC bootstrap error: {}", e.getMessage(), e);
                }
            }, "vnc-bootstrap");
            vncThread.setDaemon(true);
            vncThread.start();
        }

        AppConfig.getInstance();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.warn("Could not set system look and feel", e);
        }

        RestClient restClient = RestClient.getInstance();
        TokenManager tokenManager = TokenManager.getInstance();
        SessionManager sessionManager = SessionManager.getInstance();
        AuthApiClient authApiClient = new AuthApiClient(restClient);
        AuthService authService = new AuthService(authApiClient, tokenManager, sessionManager);

        // Let the REST interceptor transparently refresh the access token on 401.
        restClient.setTokenRefresher(authService::refreshAccessToken);

        SwingUtilities.invokeLater(() -> new LoginFrame(authService));
    }
}
