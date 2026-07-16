package com.bachld.backend.controller;

import com.bachld.backend.dto.request.ClipboardEventRequest;
import com.bachld.backend.dto.request.StudentClassInfoCreateRequest;
import com.bachld.backend.dto.response.StudentClassInfoResponse;
import com.bachld.backend.service.ScreenshotCaptureService;
import com.bachld.backend.service.TrackingService;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TrackingWebSocketController {

  static final long VIOLATION_SCREENSHOT_COOLDOWN_SECONDS = 5;

  TrackingService trackingService;

  ScreenshotCaptureService screenshotCaptureService;

  SimpMessagingTemplate messagingTemplate;

  ConcurrentHashMap<String, Instant> violationScreenshotTimes = new ConcurrentHashMap<>();

  @MessageMapping("/pc-info")
  public void handlePcInfo(StudentClassInfoCreateRequest request, Principal principal) {
    if (principal == null) {
      return;
    }

    try {
      String username = principal.getName();

      StudentClassInfoResponse response =
          trackingService.processTracking(Integer.valueOf(username), request);

      if (response != null && response.getClassId() != null) {
        if ("EXAM".equals(response.getType())) {
          messagingTemplate.convertAndSend("/topic/exam/" + response.getClassId(), response);
        } else {
          messagingTemplate.convertAndSend("/topic/class/" + response.getClassId(), response);
        }
        captureViolationScreenshot(Integer.valueOf(username), response);
      }
    } catch (Exception e) {
      log.error("!!! [WS-TEST] LỖI XỬ LÝ MESSAGE: {}", e.getMessage(), e);
    }
  }

  @MessageMapping("/clipboard-event")
  public void handleClipboardEvent(ClipboardEventRequest request, Principal principal) {
    if (principal == null) {
      return;
    }

    try {
      String username = principal.getName();
      StudentClassInfoResponse response =
          trackingService.processClipboardEvent(Integer.valueOf(username), request);

      if (response != null && response.getClassId() != null) {
        if ("EXAM".equals(response.getType())) {
          messagingTemplate.convertAndSend("/topic/exam/" + response.getClassId(), response);
        } else {
          messagingTemplate.convertAndSend("/topic/class/" + response.getClassId(), response);
        }
      }
    } catch (Exception e) {
      log.error("Cannot process clipboard event: {}", e.getMessage(), e);
    }
  }

  private void captureViolationScreenshot(
      Integer studentUserId, StudentClassInfoResponse response) {
    if (!response.isBanApplication()) {
      return;
    }

    String cooldownKey =
        response.getType()
            + ":"
            + response.getClassId()
            + ":"
            + studentUserId
            + ":"
            + response.getApplicationName();
    Instant now = Instant.now();
    Instant lastCapturedAt = violationScreenshotTimes.get(cooldownKey);
    if (lastCapturedAt != null
        && Duration.between(lastCapturedAt, now).getSeconds()
            < VIOLATION_SCREENSHOT_COOLDOWN_SECONDS) {
      return;
    }
    violationScreenshotTimes.put(cooldownKey, now);

    try {
      if ("EXAM".equals(response.getType())) {
        screenshotCaptureService.requestExamRoomViolationScreenshot(
            response.getClassId(), studentUserId);
      } else {
        screenshotCaptureService.requestClassViolationScreenshot(
            response.getClassId(), studentUserId);
      }
    } catch (Exception e) {
      log.warn(
          "Không thể tự động chụp màn hình vi phạm. studentUserId={}, contextId={}: {}",
          studentUserId,
          response.getClassId(),
          e.getMessage(),
          e);
    }
  }
}
