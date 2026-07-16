package com.bachld.backend.dto.response;

import java.time.LocalDate;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SemesterResponse {

  Integer id;

  String name;

  String studyYear;

  LocalDate startDate;

  LocalDate endDate;

  Integer status;
}
