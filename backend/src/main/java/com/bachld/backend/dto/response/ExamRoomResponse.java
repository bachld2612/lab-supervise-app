package com.bachld.backend.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamRoomResponse {

  Integer id;

  String code;

  Integer roomId;

  String roomName;

  Integer teacher1Id;

  String teacher1Name;

  Integer teacher2Id;

  String teacher2Name;

  Integer subjectId;

  String subjectName;

  Integer semesterId;

  String semesterName;

  Integer maxStudent;

  Long currentStudent;

  LocalDate examDate;

  String periods;

  LocalTime startTime;

  LocalTime endTime;

  Integer status;

  Boolean trackingEnabled;

  Integer studyStatus;

  String wifiSsid;

  public ExamRoomResponse(
      Integer id,
      String code,
      Integer roomId,
      String roomName,
      Integer teacher1Id,
      String teacher1Name,
      Integer teacher2Id,
      String teacher2Name,
      Integer subjectId,
      String subjectName,
      Integer semesterId,
      String semesterName,
      Integer maxStudent,
      Long currentStudent,
      LocalDate examDate,
      String periods,
      LocalTime startTime,
      LocalTime endTime,
      Integer status,
      Boolean trackingEnabled,
      String wifiSsid) {
    this.id = id;
    this.code = code;
    this.roomId = roomId;
    this.roomName = roomName;
    this.teacher1Id = teacher1Id;
    this.teacher1Name = teacher1Name;
    this.teacher2Id = teacher2Id;
    this.teacher2Name = teacher2Name;
    this.subjectId = subjectId;
    this.subjectName = subjectName;
    this.semesterId = semesterId;
    this.semesterName = semesterName;
    this.maxStudent = maxStudent;
    this.currentStudent = currentStudent;
    this.examDate = examDate;
    this.periods = periods;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = status;
    this.trackingEnabled = trackingEnabled;
    this.wifiSsid = wifiSsid;
  }
}
