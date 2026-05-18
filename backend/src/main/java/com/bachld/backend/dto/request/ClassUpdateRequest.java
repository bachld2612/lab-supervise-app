package com.bachld.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
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
public class ClassUpdateRequest {

    String name;

    @Min(value = 1, message = "Sĩ số tối đa phải lớn hơn 0")
    Integer maxStudent;

    Integer subjectId;

    Integer teacherId;

    Integer scheduleId;

    @Pattern(
            regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$",
            message = "Ngày bắt đầu phải có định dạng yyyy-MM-dd"
    )
    String startDate;

    @Pattern(
            regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$",
            message = "Ngày kết thúc phải có định dạng yyyy-MM-dd"
    )
    String endDate;

    Integer semesterId;

    Integer roomId;
}
