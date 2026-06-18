package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentClassInfoResponse {

    Integer classId;

    Integer studentId;

    String studentName;

    String studentCode;

    String applicationName;

    Integer action;

    String clipboardText;

    LocalDateTime createdAt;

    boolean isBanApplication;

    String type;
}
