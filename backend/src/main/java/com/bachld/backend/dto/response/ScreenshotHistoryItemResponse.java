package com.bachld.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
