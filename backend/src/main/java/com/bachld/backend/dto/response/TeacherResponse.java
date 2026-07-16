package com.bachld.backend.dto.response;

import java.time.LocalDate;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherResponse {

  Integer id;

  String email;

  String phone;

  String fullName;

  String code;

  String hometown;

  Integer sectionId;

  String sectionName;

  LocalDate birthday;

  Integer userId;

  String rawPassword;

  Integer status;
}
