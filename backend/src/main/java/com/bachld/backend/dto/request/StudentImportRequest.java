package com.bachld.backend.dto.request;

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
public class StudentImportRequest {

  @NotEmpty(message = "STT không được phép bỏ trống") String ordinal;

  @NotEmpty(message = "Mã sinh viên không được phép bỏ trống") String code;

  @NotEmpty(message = "Tên không được phép bỏ trống") String fullName;

  @NotEmpty(message = "Lớp quản lý không được phép bỏ trống") String manageClassName;
}
