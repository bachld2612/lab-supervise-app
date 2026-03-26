package com.bachld.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreateRequest {

    @NotEmpty(message = "Email không được phép bỏ trống")
    @Email(message = "Email không hợp lệ")
    String email;

    String fullName;

    String phone;

    @NotNull(message = "Role không được phép bỏ trống")
    Integer roleId;
}
