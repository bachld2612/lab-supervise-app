package com.bachld.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentPcInfoResponse {
    private Integer studentId;
    private Integer userId;
    private String fullName;
    private String code;
    private String ipAddress;
}
