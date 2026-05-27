package com.bachld.service;

import com.bachld.client.ScreenshotApiClient;
import com.bachld.config.AppConfig;
import com.bachld.config.RestClient;
import com.bachld.model.request.PCInfoPayload;
import com.bachld.model.response.FilePayload;
import com.bachld.model.response.RemoteCommandMessage;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.swing.SwingUtilities;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.Base64;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * WebSocketService manages the real-time communication with the backend server.
 * It uses STOMP over WebSocket to send tracking data.
 */
public class WebSocketService {
    private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);

    private final WebSocketStompClient stompClient;
    private StompSession stompSession;
    private final TokenManager tokenManager;
    private final String wsUrl;
    private final RemoteCommandExecutor remoteCommandExecutor = new RemoteCommandExecutor();
    private final ScreenshotApiClient screenshotApiClient = new ScreenshotApiClient(RestClient.getInstance());
    private final Object sendLock = new Object();
    private static final String SCREENSHOT_COMMAND = "SCREENSHOT";
    
    private static volatile WebSocketService instance;

    /**
     * Private constructor for singleton pattern.
     * @param tokenManager the token manager to retrieve authentication tokens
     */
    private WebSocketService(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
        this.wsUrl = AppConfig.getInstance().getServerWsUrl();

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(50 * 1024 * 1024);
        container.setDefaultMaxBinaryMessageBufferSize(50 * 1024 * 1024);

        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient(container));
        this.stompClient.setInboundMessageSizeLimit(50 * 1024 * 1024);
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }
    
    /**
     * Returns the singleton instance of WebSocketService.
     * @param tokenManager the token manager
     * @return the singleton instance
     */
    public static WebSocketService getInstance(TokenManager tokenManager) {
        if (instance == null) {
            synchronized (WebSocketService.class) {
                if (instance == null) {
                    instance = new WebSocketService(tokenManager);
                }
            }
        }
        return instance;
    }

    /**
     * Establishes a connection to the WebSocket server.
     * Includes authentication token in handshake headers if available.
     */
    public synchronized void connect() {
        if (stompSession != null && stompSession.isConnected()) {
            return;
        }

        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        if (tokenManager.hasToken()) {
            // Handshake header (some servers use this, others use STOMP connect headers)
            handshakeHeaders.add("Authorization", "Bearer " + tokenManager.getToken());
        }

        // STOMP CONNECT headers (often required by Spring Security WebSocket)
        StompHeaders connectHeaders = new StompHeaders();
        if (tokenManager.hasToken()) {
            connectHeaders.add("Authorization", "Bearer " + tokenManager.getToken());
        }

        StompSessionHandler sessionHandler = new MyStompSessionHandler();

        try {
            // Connect asynchronously with timeout
            stompSession = stompClient.connectAsync(wsUrl, handshakeHeaders, connectHeaders, sessionHandler)
                    .get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("WebSocket connection interrupted.");
        } catch (ExecutionException | TimeoutException e) {
            log.error("Could not connect to WebSocket: {}", e.getMessage());
        }
    }

    public void sendPCInfo(String applicationName) {
        PCInfoPayload payload = new PCInfoPayload(applicationName);
        if (!sendToServer("/app/pc-info", payload)) {
            log.warn("WebSocket disconnected. PC info update lost: {}", applicationName);
        }
    }

    /**
     * Returns the connection status.
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return stompSession != null && stompSession.isConnected();
    }

    /**
     * Disconnects from the WebSocket server.
     */
    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
    }

    private void saveReceivedFile(FilePayload payload) {
        try {
            String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
            File dir = new File(downloadsPath);
            if (!dir.exists()) dir.mkdirs();

            byte[] fileBytes = Base64.getDecoder().decode(payload.getFileContentBase64());
            File outputFile = new File(dir, payload.getFileName());
            Files.write(outputFile.toPath(), fileBytes);

            SwingUtilities.invokeLater(() -> {
                if (SystemTray.isSupported()) {
                    TrayIcon[] icons = SystemTray.getSystemTray().getTrayIcons();
                    if (icons.length > 0) {
                        icons[0].displayMessage(
                                "File nhận được",
                                "\"" + payload.getFileName() + "\" đã được lưu vào Downloads.",
                                TrayIcon.MessageType.INFO
                        );
                    }
                }
            });
        } catch (IllegalArgumentException e) {
            log.error("Base64 payload không hợp lệ: {}", e.getMessage());
        } catch (IOException e) {
            log.error("Không thể lưu file nhận được: {}", e.getMessage());
        }
    }

    /**
     * Inner handler to monitor STOMP session events.
     */
    private class MyStompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            log.info("STOMP connection successful. SessionID: {}", session.getSessionId());

            com.bachld.model.response.User currentUser =
                    SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                String topic = "/topic/user/" + currentUser.getId() + "/file";
                session.subscribe(topic, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return FilePayload.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        saveReceivedFile((FilePayload) payload);
                    }
                });
                log.info("Subscribed to {}", topic);

                String commandTopic = "/topic/user/" + currentUser.getId() + "/command";
                session.subscribe(commandTopic, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return RemoteCommandMessage.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        handleRemoteCommand((RemoteCommandMessage) payload);
                    }
                });
                log.info("Subscribed to {}", commandTopic);
            }
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            log.error("STOMP protocol error: {}", exception.getMessage());
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            log.warn("WebSocket transport error. Session may be lost: {}", exception.getMessage());
        }
    }

    private void handleRemoteCommand(RemoteCommandMessage command) {
        Thread worker = new Thread(() -> {
            if (command == null || !SCREENSHOT_COMMAND.equals(command.getType())) {
                log.warn("Unsupported remote command: {}", command == null ? null : command.getType());
                return;
            }

            Integer screenshotId = extractScreenshotId(command);
            if (screenshotId == null) {
                log.warn("Screenshot command missing screenshotId. commandId={}", command.getCommandId());
                return;
            }

            try {
                byte[] imageBytes = remoteCommandExecutor.captureScreenshotJpeg();
                screenshotApiClient.uploadScreenshot(screenshotId, imageBytes);
                log.info("Uploaded screenshot {}", screenshotId);
            } catch (Exception e) {
                log.warn("Screenshot command failed. screenshotId={}: {}", screenshotId, e.getMessage(), e);
            }
        }, "remote-command-" + (command == null ? "unknown" : command.getCommandId()));
        worker.setDaemon(true);
        worker.start();
    }

    private Integer extractScreenshotId(RemoteCommandMessage command) {
        if (command.getArguments() == null) {
            return null;
        }

        Object value = command.getArguments().get("screenshotId");
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean sendToServer(String destination, Object payload) {
        synchronized (sendLock) {
            StompSession session = stompSession;
            if (session == null || !session.isConnected()) {
                return false;
            }

            try {
                session.send(destination, payload);
                return true;
            } catch (Exception e) {
                log.warn("STOMP send failed to {}: {}", destination, e.getMessage());
                return false;
            }
        }
    }
}
