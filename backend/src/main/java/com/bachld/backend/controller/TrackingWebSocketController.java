package com.bachld.backend.controller;

import com.bachld.backend.dto.request.StudentClassInfoCreateRequest;
import com.bachld.backend.dto.response.StudentClassInfoResponse;
import com.bachld.backend.service.TrackingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalTime;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,  makeFinal = true)
@Slf4j
public class TrackingWebSocketController {

    TrackingService trackingService;

    SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/pc-info")
    public void handlePcInfo(StudentClassInfoCreateRequest request, Principal principal) {
        if (principal == null) {
            return;
        }

        try {
            String username = principal.getName();
            String appName = request.getApplicationName();

            StudentClassInfoResponse response = trackingService.processTracking(Integer.valueOf(username), request);

            if (response != null && response.getClassId() != null) {
                messagingTemplate.convertAndSend("/topic/class/" + response.getClassId(), response);
            } else {
                log.error("!!! [WS-TEST] Sinh viên đang sử dụng: {} vào lúc {}", appName, LocalTime.now());
            }
        } catch (Exception e) {
            log.error("!!! [WS-TEST] LỖI XỬ LÝ MESSAGE: {}", e.getMessage(), e);
        }
    }
}
