package com.bachld.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExamRoomListResponse {
    private int statusCode;
    private List<ExamRoomData> data;
}
