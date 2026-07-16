package com.bachld.backend.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
