package com.bachld.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
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

    @NotEmpty(message = "Mật khẩu không được phép bỏ trống")
    String password;

    String fullName;

    String phone;
}
