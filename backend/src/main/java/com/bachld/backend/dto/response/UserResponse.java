package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {

    Integer id;

    String email;

    String phone;

    String fullName;

    String hometown;

    LocalDate birthday;

    String rawPassword;

    Integer roleId;

    String roleName;

    String roleColor;

    Integer status;
}
