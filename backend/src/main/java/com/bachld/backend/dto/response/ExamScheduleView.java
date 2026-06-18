package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

/** Projection of an exam's date + time window, for schedule conflict checks. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamScheduleView {

    LocalDate examDate;

    LocalTime startTime;

    LocalTime endTime;
}
