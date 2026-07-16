package com.bachld.backend.dto.response;

import java.time.LocalDate;
import lombok.*;
import lombok.experimental.FieldDefaults;

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

  LocalDate startDate;

  LocalDate endDate;

  Integer semesterId;

  String semesterName;

  Integer studyStatus;

  Integer roomId;

  String roomName;

  String wifiSsid;

  Boolean trackingEnabled;

  public ClassResponse(
      Integer id,
      String name,
      Integer currentStudent,
      Integer maxStudent,
      Integer sessionNumber,
      Integer status,
      Integer subjectId,
      String subjectName,
      Integer teacherId,
      String teacherName,
      Integer scheduleId,
      String scheduleName,
      LocalDate startDate,
      LocalDate endDate,
      Integer semesterId,
      String semesterName,
      Integer roomId,
      String roomName,
      String wifiSsid) {
    this(
        id,
        name,
        currentStudent,
        maxStudent,
        sessionNumber,
        status,
        subjectId,
        subjectName,
        teacherId,
        teacherName,
        scheduleId,
        scheduleName,
        startDate,
        endDate,
        semesterId,
        semesterName,
        roomId,
        roomName,
        wifiSsid,
        true);
  }

  public ClassResponse(
      Integer id,
      String name,
      Integer currentStudent,
      Integer maxStudent,
      Integer sessionNumber,
      Integer status,
      Integer subjectId,
      String subjectName,
      Integer teacherId,
      String teacherName,
      Integer scheduleId,
      String scheduleName,
      LocalDate startDate,
      LocalDate endDate,
      Integer semesterId,
      String semesterName,
      Integer roomId,
      String roomName,
      String wifiSsid,
      Boolean trackingEnabled) {
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
    this.roomId = roomId;
    this.roomName = roomName;
    this.wifiSsid = wifiSsid;
    this.trackingEnabled = trackingEnabled;
  }
}
