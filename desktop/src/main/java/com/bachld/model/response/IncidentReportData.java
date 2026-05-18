package com.bachld.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncidentReportData {
    private int id;
    private String title;
    private int status; // 0=chờ xử lý, 1=đã xử lý, 2=từ chối
    private String roomName;
    private String reporterName;
    private String createdAt;
}