package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

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
