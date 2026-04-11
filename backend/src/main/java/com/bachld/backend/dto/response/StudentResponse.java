package com.bachld.backend.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
