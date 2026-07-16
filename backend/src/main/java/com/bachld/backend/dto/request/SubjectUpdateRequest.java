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
public class SubjectUpdateRequest {

  String name;

  String code;

  @Min(value = 1, message = "Số tín chỉ phải lớn hơn 1") Integer creditNumber;

  Integer sectionId;
}
