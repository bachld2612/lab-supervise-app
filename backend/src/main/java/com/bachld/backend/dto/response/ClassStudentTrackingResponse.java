package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassStudentTrackingResponse {

    Integer studentId;

    String fullName;

    String code;

    String email;

    String phone;

    Integer manageClassId;

    String manageClassName;
}