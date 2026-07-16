package com.bachld.backend.dto.response;

import java.time.LocalTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
