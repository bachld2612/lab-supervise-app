package com.bachld.backend.service;

import com.bachld.backend.dto.websocket.RemoteCommandMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemoteCommandService {

    private static final String SCREENSHOT_COMMAND = "SCREENSHOT";

    private final SimpMessagingTemplate messagingTemplate;

    public void sendScreenshotCommand(Integer studentUserId, Integer screenshotId) {
        RemoteCommandMessage message = RemoteCommandMessage.builder()
                .commandId(UUID.randomUUID().toString())
                .type(SCREENSHOT_COMMAND)
                .arguments(Map.of("screenshotId", screenshotId))
                .build();

        messagingTemplate.convertAndSend("/topic/user/" + studentUserId + "/command", message);
    }
}
