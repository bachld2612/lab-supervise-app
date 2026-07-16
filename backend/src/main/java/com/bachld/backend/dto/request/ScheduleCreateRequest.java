package com.bachld.backend.dto.request;

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
public class ScheduleCreateRequest {

  @NotEmpty(message = "Tên lịch học không được phép bỏ trống") String name;

  @NotEmpty(message = "Ngày trong tuần không được phép bỏ trống") String daysOfWeek;

  @NotEmpty(message = "Tiết học không được phép bỏ trống") @Pattern(
      regexp = "^([1-9]|1[0-2])(,[1-9]|,1[0-2])*$",
      message = "Định dạng tiết học không hợp lệ.")
  String periods;
}
