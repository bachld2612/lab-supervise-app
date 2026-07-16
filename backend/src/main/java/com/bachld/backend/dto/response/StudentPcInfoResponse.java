package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentPcInfoResponse {

  private Integer studentId;

  private Integer userId;

  private String fullName;

  private String code;

  private String ipAddress;
}
