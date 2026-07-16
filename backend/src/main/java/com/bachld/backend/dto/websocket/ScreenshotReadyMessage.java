package com.bachld.backend.dto.websocket;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScreenshotReadyMessage {

  String type;

  Integer screenshotId;

  Integer studentId;

  Integer studentUserId;

  String imageUrl;

  LocalDateTime createdAt;
}
