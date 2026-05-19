package com.bachld.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExamRoomData {
    private Long id;
    private String code;
    private int currentStudent;
    private int maxStudent;
    private int status;
    private Long subjectId;
    private String subjectName;
    private Long teacher1Id;
    private String teacher1Name;
    private Long teacher2Id;
    private String teacher2Name;
    private Long roomId;
    private String roomName;
    private Long semesterId;
    private String semesterName;
    private String examDate;
    private String startTime;
    private String endTime;
}
