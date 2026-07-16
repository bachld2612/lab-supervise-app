package com.bachld.backend.dto.response;

import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScreenshotHistoryItemResponse {

  Integer id;

  LocalDateTime createdAt;

  Integer studentId;

  String studentName;

  String studentCode;

  String contextType;

  Integer contextId;

  String contextName;

  String applicationName;

  String imageUrl;
}
