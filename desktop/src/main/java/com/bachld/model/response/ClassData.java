package com.bachld.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassData {
    private Long id;
    private String name;
    private int currentStudent;
    private int maxStudent;
    private int sessionNumber;
    private int status;
    private Long subjectId;
    private String subjectName;
    private Long teacherId;
    private String teacherName;
    private Long scheduleId;
    private String scheduleName;
    private String startDate;
    private String endDate;
    private Long semesterId;
    private String semesterName;
    private int studyStatus; // 0: Upcoming, 1: Ongoing
}
