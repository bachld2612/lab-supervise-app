package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassResponse {

    Integer id;

    String name;

    Integer currentStudent;

    Integer maxStudent;

    Integer sessionNumber;

    Integer status;

    Integer subjectId;

    String subjectName;

    Integer teacherId;

    String teacherName;

    Integer scheduleId;

    String scheduleName;

    LocalDate  startDate;

    LocalDate endDate;

    Integer semesterId;

    String semesterName;

    Integer studyStatus;

    public ClassResponse(Integer id, String name, Integer currentStudent, Integer maxStudent, Integer sessionNumber, Integer status, Integer subjectId, String subjectName, Integer teacherId, String teacherName, Integer scheduleId, String scheduleName, LocalDate startDate, LocalDate endDate, Integer semesterId, String semesterName) {
        this.id = id;
        this.name = name;
        this.currentStudent = currentStudent;
        this.maxStudent = maxStudent;
        this.sessionNumber = sessionNumber;
        this.status = status;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.scheduleId = scheduleId;
        this.scheduleName = scheduleName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.semesterId = semesterId;
        this.semesterName = semesterName;
    }
}
