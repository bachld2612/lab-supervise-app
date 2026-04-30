package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentClassInfoResponse {

    Integer classId;

    Integer studentId;

    String studentName;

    String studentCode;

    String applicationName;

    LocalDateTime createdAt;

    boolean isBanApplication;
}
