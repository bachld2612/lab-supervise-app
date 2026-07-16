package com.bachld.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class UserCreateRequest {

  @NotEmpty(message = "Email không được phép bỏ trống") @Email(message = "Email không hợp lệ") String email;

  @NotEmpty(message = "Tên không được phép bỏ trống") String fullName;

  @NotEmpty(message = "Quê nhà không được phép bỏ trống") String hometown;

  @NotEmpty(message = "Số điện thoại không được phép bỏ trống") String phone;

  @NotEmpty(message = "Ngày sinh không được phép bỏ trống") @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Ngày sinh phải có định dạng yyyy-MM-dd") String birthday;

  @NotNull(message = "Role không được phép bỏ trống") Integer roleId;
}
