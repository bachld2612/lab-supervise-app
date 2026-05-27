package com.bachld.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScreenshotStudentOptionResponse {
    Integer studentId;
    String fullName;
    String code;
}
