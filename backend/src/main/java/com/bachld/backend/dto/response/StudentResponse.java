package com.bachld.backend.dto.response;

import java.time.LocalDate;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentResponse {

  Integer id;

  String email;

  String phone;

  String fullName;

  String code;

  Integer manageClassId;

  String manageClassName;

  String hometown;

  LocalDate birthday;

  String rawPassword;

  Integer status;
}
