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
    private static final String OPEN_WEBSITE_COMMAND = "OPEN_WEBSITE";
    private static final String SHOW_MESSAGE_COMMAND = "SHOW_MESSAGE";
    private static final String LOCK_SCREEN_COMMAND = "LOCK_SCREEN";
    private static final String FILE_AVAILABLE_COMMAND = "FILE_AVAILABLE";

    private final SimpMessagingTemplate messagingTemplate;

    public void sendScreenshotCommand(Integer studentUserId, Integer screenshotId) {
        RemoteCommandMessage message = RemoteCommandMessage.builder()
                .commandId(UUID.randomUUID().toString())
                .type(SCREENSHOT_COMMAND)
                .arguments(Map.of("screenshotId", screenshotId))
                .build();

        messagingTemplate.convertAndSend("/topic/user/" + studentUserId + "/command", message);
    }

    public void sendOpenWebsiteCommand(Integer studentUserId, String websiteUrl) {
        sendCommand(studentUserId, OPEN_WEBSITE_COMMAND, Map.of("websiteUrl", websiteUrl));
    }

    public void sendShowMessageCommand(Integer studentUserId, String text) {
        sendCommand(studentUserId, SHOW_MESSAGE_COMMAND, Map.of("text", text));
    }

    public void sendLockScreenCommand(Integer studentUserId, boolean active) {
        sendCommand(studentUserId, LOCK_SCREEN_COMMAND, Map.of("active", active));
    }

    public void sendFileAvailableCommand(Integer studentUserId, String fileToken, String fileName, Long fileSize) {
        sendCommand(studentUserId, FILE_AVAILABLE_COMMAND, Map.of(
                "fileToken", fileToken,
                "fileName", fileName,
                "fileSize", fileSize
        ));
    }

    private void sendCommand(Integer studentUserId, String type, Map<String, Object> arguments) {
        RemoteCommandMessage message = RemoteCommandMessage.builder()
                .commandId(UUID.randomUUID().toString())
                .type(type)
                .arguments(arguments)
                .build();

        messagingTemplate.convertAndSend("/topic/user/" + studentUserId + "/command", message);
    }
}
