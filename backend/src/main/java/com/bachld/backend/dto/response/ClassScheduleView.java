package com.bachld.backend.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Projection of a class's study period + weekly schedule, for conflict checks. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassScheduleView {

  LocalDate startDate;

  LocalDate endDate;

  String daysOfWeek;

  LocalTime startTime;

  LocalTime endTime;
}
