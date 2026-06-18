package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IncidentReportResponse {

    Integer id;

    String title;

    Integer status; // 0=chờ xử lý, 1=đã xử lý, 2=từ chố

    Integer roomId;

    String roomName;

    Integer reporterId;

    String reporterName;

    String reporterRole;

    Integer handlerId;

    String handlerName;

    LocalDateTime createdAt;
}