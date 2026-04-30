package com.bachld.service;

import com.bachld.config.AppConfig;
import com.bachld.model.request.PCInfoPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

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
    
    private static volatile WebSocketService instance;

    /**
     * Private constructor for singleton pattern.
     * @param tokenManager the token manager to retrieve authentication tokens
     */
    private WebSocketService(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
        this.wsUrl = AppConfig.getInstance().getServerWsUrl();

        // Initialize STOMP client with standard WebSocket client
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        // Use Jackson for JSON conversion (consistent with RestClient)
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

    /**
     * Sends the currently active application name to the server.
     * @param applicationName the name of the active window/application
     */
    public void sendPCInfo(String applicationName) {
        if (stompSession != null && stompSession.isConnected()) {
            PCInfoPayload payload = new PCInfoPayload(applicationName);
            // Sending to /app prefix triggers @MessageMapping in Spring controller
            stompSession.send("/app/pc-info", payload);
        } else {
            log.warn("WebSocket disconnected. PC info update lost: {}", applicationName);
            // Attempt to reconnect in background if allowed by business logic
            // for now just log and wait for next event or manual reconnect
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

    /**
     * Inner handler to monitor STOMP session events.
     */
    private class MyStompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            log.info("STOMP connection successful. SessionID: {}", session.getSessionId());
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
}
