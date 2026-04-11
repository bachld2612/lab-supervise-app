package com.bachld.backend.dto.request;

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
public class ScheduleUpdateRequest {

    String name;

    String daysOfWeek;

    @Pattern(regexp = "^([1-9]|1[0-2])(,[1-9]|,1[0-2])*$", message = "Định dạng tiết học không hợp lệ.")
    String periods;
}
