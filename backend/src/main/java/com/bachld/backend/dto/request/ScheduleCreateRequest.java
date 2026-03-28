package com.bachld.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduleCreateRequest {

    @NotEmpty(message = "Tên lịch học không được phép bỏ trống")
    String name;

    @NotNull(message = "Số tiết không được phép bỏ trống")
    @Min(value = 1, message = "Số tiết phải lớn hơn 0")
    Integer sessionCount;

    @NotEmpty(message = "Ngày trong tuần không được phép bỏ trống")
    String daysOfWeek;

    @NotNull(message = "Giờ bắt đầu không được phép bỏ trống")
    LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được phép bỏ trống")
    LocalTime endTime;
}
