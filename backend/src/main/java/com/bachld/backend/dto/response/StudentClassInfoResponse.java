package com.bachld.backend.dto.response;

import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
