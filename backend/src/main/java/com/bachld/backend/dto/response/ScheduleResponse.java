package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduleResponse {

    Integer id;

    String name;

    String daysOfWeek;

    String periods;

    LocalTime startTime;

    LocalTime endTime;

    Integer status;
}
