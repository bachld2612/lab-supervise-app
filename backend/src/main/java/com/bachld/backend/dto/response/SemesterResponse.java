package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

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
