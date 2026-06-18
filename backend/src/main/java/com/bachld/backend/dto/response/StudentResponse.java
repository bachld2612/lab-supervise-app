package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

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
