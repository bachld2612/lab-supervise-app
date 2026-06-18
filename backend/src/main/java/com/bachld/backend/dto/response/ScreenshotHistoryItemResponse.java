package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
