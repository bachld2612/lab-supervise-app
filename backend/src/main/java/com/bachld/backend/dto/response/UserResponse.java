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
public class UserResponse {

    Integer id;

    String email;

    String phone;

    String fullName;

    String hometown;

    LocalDate birthDay;

    String rawPassword;

    Integer roleId;

    String roleName;

    String roleColor;

    Integer status;
}
