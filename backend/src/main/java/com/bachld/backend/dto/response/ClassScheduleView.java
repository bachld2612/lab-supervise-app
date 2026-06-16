package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

/** Projection of a class's study period + weekly schedule, for conflict checks. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassScheduleView {

    LocalDate startDate;

    LocalDate endDate;

    String daysOfWeek;

    LocalTime startTime;

    LocalTime endTime;
}
