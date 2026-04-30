package com.bachld.backend.dto.request;

import jakarta.validation.constraints.Min;
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
public class ClassCreateRequest {

    @NotEmpty(message = "Tên lớp không được bỏ trống")
    String name;

    @NotNull(message = "Sĩ số tối đa không được phép bỏ trống")
    @Min(value = 1, message = "Sĩ số tối đa phải lớn hơn 0")
    Integer maxStudent;

    @NotNull(message = "Môn học không được phép bỏ trống")
    Integer subjectId;

    @NotNull(message = "Giảng viên không được phép bỏ trống")
    Integer teacherId;

    @NotNull(message = "Lịch học không được phép bỏ trống")
    Integer scheduleId;

    @Pattern(
            regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$",
            message = "Ngày bắt đầu phải có định dạng yyyy-MM-dd"
    )
    @NotEmpty(message = "Ngày bắt đầu không được phép bỏ trống")
    String startDate;

    @Pattern(
            regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$",
            message = "Ngày kết thúc phải có định dạng yyyy-MM-dd"
    )
    @NotEmpty(message = "Ngày kết thúc không được phép bỏ trống")
    String endDate;

    @NotNull(message = "Học kỳ không được phép bỏ trống")
    Integer semesterId;
}
