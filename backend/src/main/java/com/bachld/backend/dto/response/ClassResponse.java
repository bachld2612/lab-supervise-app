package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassResponse {

    Integer id;

    String name;

    Integer maxStudent;

    Integer sessionNumber;

    Integer status;

    String subjectName;

    String teacherName;

    String scheduleName;
}
