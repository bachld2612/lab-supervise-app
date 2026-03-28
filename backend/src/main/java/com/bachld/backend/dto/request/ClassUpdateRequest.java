package com.bachld.backend.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassUpdateRequest {

    String name;

    @Min(value = 1, message = "Sĩ số tối đa phải lớn hơn 0")
    Integer maxStudent;

    Integer subjectId;

    Integer teacherId;

    Integer scheduleId;
}
