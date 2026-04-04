package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ManageClassResponse {

    Integer id;

    String name;

    Integer maxStudent;

    Integer status;

    String teacherName;

    Integer teacherId;

    String majorName;

    Integer majorId;
}
