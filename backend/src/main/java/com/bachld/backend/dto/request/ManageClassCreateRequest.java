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
public class ManageClassCreateRequest {

    @NotEmpty(message = "Tên lớp không được phép bỏ trống")
    String name;

    @NotNull(message = "Sĩ số tối đa không được phép bỏ trống")
    @Min(value = 1, message = "Sĩ số tối đa phải lớn hơn 1")
    Integer maxStudent;

    @NotNull(message = "Giảng viên không được phép bỏ trống")
    Integer teacherId;

    @NotNull(message = "Chuyên ngành không được phép bỏ trống")
    Integer majorId;
}
