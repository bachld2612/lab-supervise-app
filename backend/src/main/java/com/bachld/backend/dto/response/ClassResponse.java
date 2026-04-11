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

    Integer currentStudent;

    Integer maxStudent;

    Integer sessionNumber;

    Integer status;

    Integer subjectId;

    String subjectName;

    Integer teacherId;

    String teacherName;

    Integer scheduleId;

    String scheduleName;
}
