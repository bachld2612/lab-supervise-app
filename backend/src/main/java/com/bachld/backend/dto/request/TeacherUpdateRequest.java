package com.bachld.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherUpdateRequest {

  @Email(message = "Email không hợp lệ") String email;

  String code;

  String fullName;

  String hometown;

  @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Ngày sinh phải có định dạng yyyy-MM-dd") String birthday;

  Integer sectionId;

  String phone;
}
