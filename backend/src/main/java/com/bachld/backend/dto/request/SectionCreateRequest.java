package com.bachld.backend.dto.request;

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
public class SectionCreateRequest {

  @NotEmpty(message = "Bộ môn không được phép bỏ trống") String name;

  @NotNull(message = "Khoa không được phép bỏ trống") Integer departmentId;
}
