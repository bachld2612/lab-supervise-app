package com.bachld.service;

import com.bachld.client.FileDownloadApiClient;
import com.bachld.client.ScreenshotApiClient;
import com.bachld.config.AppConfig;
import com.bachld.config.RestClient;
import com.bachld.model.request.ClipboardEventPayload;
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
import java.util.Map;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocketService manages the real-time communication with the backend server.
 * It uses STOMP over WebSocket to send tracking data.
 */
public class WebSocketService {
    private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);

    private final WebSocketStompClient stompClient;
    private StompSession stompSession;
    private final TokenManager tokenManager;
    private volatile com.bachld.config.TokenRefresher tokenRefresher;
    private final String wsUrl;
    private final RemoteCommandExecutor remoteCommandExecutor = new RemoteCommandExecutor();
    private final ScreenshotApiClient screenshotApiClient = new ScreenshotApiClient(RestClient.getInstance());
    private final FileDownloadApiClient fileDownloadApiClient = new FileDownloadApiClient(RestClient.getInstance());
    private final ClipboardEncryptionService clipboardEncryptionService = new ClipboardEncryptionService();
    private final ExecutorService commandExecutor;
    private final Object sendLock = new Object();
    private volatile String cachedPublicKeyBase64;
    private static final String SCREENSHOT_COMMAND = "SCREENSHOT";
    private static final String OPEN_WEBSITE_COMMAND = "OPEN_WEBSITE";
    private static final String SHOW_MESSAGE_COMMAND = "SHOW_MESSAGE";
    private static final String LOCK_SCREEN_COMMAND = "LOCK_SCREEN";
    private static final String FILE_AVAILABLE_COMMAND = "FILE_AVAILABLE";
    
    private static volatile WebSocketService instance;

    /**
     * Private constructor for singleton pattern.
     * @param tokenManager the token manager to retrieve authentication tokens
     */
    private WebSocketService(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
        AppConfig appConfig = AppConfig.getInstance();
        this.wsUrl = appConfig.getServerWsUrl();
        this.commandExecutor = Executors.newFixedThreadPool(
                appConfig.getRemoteCommandThreadPoolSize(),
                new RemoteCommandThreadFactory()
        );

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
    public void setTokenRefresher(com.bachld.config.TokenRefresher tokenRefresher) {
        this.tokenRefresher = tokenRefresher;
    }

    public synchronized void connect() {
        if (stompSession != null && stompSession.isConnected()) {
            return;
        }

        // Ensure a fresh, non-expired access token before the STOMP handshake
        // (the server validates the token only at CONNECT time).
        if (tokenRefresher != null) {
            String refreshed = tokenRefresher.refresh();
            if (refreshed != null && !refreshed.isEmpty()) {
                tokenManager.setToken(refreshed);
            }
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

    public void sendClipboardEvent(String applicationName, int action, String clipboardText) {
        try {
            String publicKey = getPublicKeyBase64();
            ClipboardEventPayload payload = clipboardEncryptionService.encrypt(applicationName, action, clipboardText, publicKey);
            if (!sendToServer("/app/clipboard-event", payload)) {
                log.warn("WebSocket disconnected. Clipboard event lost. action={}", action);
            }
        } catch (Exception e) {
            log.warn("Could not send clipboard event. action={}: {}", action, e.getMessage());
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
        remoteCommandExecutor.setScreenLocked(false);
        cachedPublicKeyBase64 = null;
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
    }

    private String getPublicKeyBase64() {
        String cached = cachedPublicKeyBase64;
        if (cached != null && !cached.isBlank()) {
            return cached;
        }

        synchronized (this) {
            if (cachedPublicKeyBase64 != null && !cachedPublicKeyBase64.isBlank()) {
                return cachedPublicKeyBase64;
            }
            String baseUrl = RestClient.getInstance().getBaseUrl();
            String url = baseUrl.endsWith("/api")
                    ? baseUrl + "/security/v1/public-key"
                    : baseUrl + "/api/security/v1/public-key";
            Map<?, ?> response = RestClient.getInstance().getRestTemplate().getForObject(url, Map.class);
            Object data = response == null ? null : response.get("data");
            if (data == null || data.toString().isBlank()) {
                throw new IllegalStateException("Server public key is empty");
            }
            cachedPublicKeyBase64 = data.toString();
            return cachedPublicKeyBase64;
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
        commandExecutor.submit(() -> {
            if (command == null) {
                log.warn("Unsupported remote command: null");
                return;
            }

            try {
                switch (command.getType()) {
                    case SCREENSHOT_COMMAND -> handleScreenshotCommand(command);
                    case OPEN_WEBSITE_COMMAND -> handleOpenWebsiteCommand(command);
                    case SHOW_MESSAGE_COMMAND -> handleShowMessageCommand(command);
                    case LOCK_SCREEN_COMMAND -> handleLockScreenCommand(command);
                    case FILE_AVAILABLE_COMMAND -> handleFileAvailableCommand(command);
                    default -> log.warn("Unsupported remote command: {}", command.getType());
                }
            } catch (Exception e) {
                log.warn("Remote command failed. commandId={}, type={}: {}", command.getCommandId(), command.getType(), e.getMessage(), e);
            }
        });
    }

    private void handleScreenshotCommand(RemoteCommandMessage command) throws Exception {
        Integer screenshotId = extractScreenshotId(command);
        if (screenshotId == null) {
            log.warn("Screenshot command missing screenshotId. commandId={}", command.getCommandId());
            return;
        }

        byte[] imageBytes = remoteCommandExecutor.captureScreenshotJpeg();
        screenshotApiClient.uploadScreenshot(screenshotId, imageBytes);
        log.info("Uploaded screenshot {}", screenshotId);
    }

    private void handleOpenWebsiteCommand(RemoteCommandMessage command) throws Exception {
        String websiteUrl = extractStringArgument(command, "websiteUrl");
        if (websiteUrl == null) {
            log.warn("Open website command missing websiteUrl. commandId={}", command.getCommandId());
            return;
        }
        remoteCommandExecutor.openWebsite(websiteUrl);
    }

    private void handleShowMessageCommand(RemoteCommandMessage command) {
        String text = extractStringArgument(command, "text");
        if (text == null) {
            log.warn("Show message command missing text. commandId={}", command.getCommandId());
            return;
        }
        remoteCommandExecutor.showMessage(text);
    }

    private void handleLockScreenCommand(RemoteCommandMessage command) {
        Boolean active = extractBooleanArgument(command, "active");
        if (active == null) {
            log.warn("Lock screen command missing active flag. commandId={}", command.getCommandId());
            return;
        }
        remoteCommandExecutor.setScreenLocked(active);
    }

    private void handleFileAvailableCommand(RemoteCommandMessage command) {
        String fileToken = extractStringArgument(command, "fileToken");
        String fileName = extractStringArgument(command, "fileName");
        if (fileToken == null) {
            Integer legacyFileId = extractIntegerArgument(command, "fileId");
            fileToken = legacyFileId == null ? null : legacyFileId.toString();
        }
        if (fileToken == null) {
            log.warn("File available command missing fileToken. commandId={}", command.getCommandId());
            return;
        }

        java.nio.file.Path savedPath = fileDownloadApiClient.downloadSharedFile(fileToken, fileName);
        log.info("Downloaded shared file {} to {}", fileToken, savedPath);
        notifyFileSaved(fileName == null ? savedPath.getFileName().toString() : fileName);
    }

    private Integer extractScreenshotId(RemoteCommandMessage command) {
        return extractIntegerArgument(command, "screenshotId");
    }

    private Integer extractIntegerArgument(RemoteCommandMessage command, String name) {
        if (command.getArguments() == null) {
            return null;
        }

        Object value = command.getArguments().get(name);
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

    private String extractStringArgument(RemoteCommandMessage command, String name) {
        if (command.getArguments() == null) {
            return null;
        }

        Object value = command.getArguments().get(name);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Boolean extractBooleanArgument(RemoteCommandMessage command, String name) {
        if (command.getArguments() == null) {
            return null;
        }

        Object value = command.getArguments().get(name);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            String normalized = text.trim().toLowerCase();
            if ("true".equals(normalized)) {
                return true;
            }
            if ("false".equals(normalized)) {
                return false;
            }
        }
        return null;
    }

    private void notifyFileSaved(String fileName) {
        SwingUtilities.invokeLater(() -> {
            if (SystemTray.isSupported()) {
                TrayIcon[] icons = SystemTray.getSystemTray().getTrayIcons();
                if (icons.length > 0) {
                    icons[0].displayMessage(
                            "File nhận được",
                            "\"" + fileName + "\" đã được lưu vào Downloads.",
                            TrayIcon.MessageType.INFO
                    );
                }
            }
        });
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

    private static class RemoteCommandThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "remote-command-worker-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
