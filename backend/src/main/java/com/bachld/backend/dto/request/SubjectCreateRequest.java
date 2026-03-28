package com.bachld.backend.dto.request;

import jakarta.validation.constraints.Min;
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
public class SubjectCreateRequest {

    @NotEmpty(message = "Tên môn học không được phép bỏ trống")
    String name;

    @NotEmpty(message = "Mã môn học không được phép bỏ trống")
    String code;

    @NotNull(message = "Số tín chỉ không được phép bỏ trống")
    @Min(value = 1, message = "Số tín chỉ phải lớn hơn 1")
    Integer creditNumber;

    @NotNull(message = "Bộ môn không được phép bỏ trống")
    Integer sectionId;
}
