package com.bachld.backend.dto.response;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassStudentTrackingResponse {

  Integer studentId;

  Integer userId;

  String fullName;

  String code;

  String email;

  String phone;

  Integer manageClassId;

  String manageClassName;

  List<AppUsageItem> applicationsToday;

  public ClassStudentTrackingResponse(
      Integer studentId,
      Integer userId,
      String fullName,
      String code,
      String email,
      String phone,
      Integer manageClassId,
      String manageClassName) {
    this.studentId = studentId;
    this.userId = userId;
    this.fullName = fullName;
    this.code = code;
    this.email = email;
    this.phone = phone;
    this.manageClassId = manageClassId;
    this.manageClassName = manageClassName;
  }
}
