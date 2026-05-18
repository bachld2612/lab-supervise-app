package com.bachld.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncidentReportListResponse {
    private int statusCode;
    private PageData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageData {
        private List<IncidentReportData> content;
        private int totalElements;
    }
}