package com.bachld.backend.config;

import com.bachld.backend.websocket.VncWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class VncWebSocketConfig implements WebSocketConfigurer {

    private final VncWebSocketHandler vncWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(vncWebSocketHandler, "/vnc-relay")
                .setAllowedOriginPatterns("*");
    }
}
