package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectResponse {

  Integer id;

  String name;

  String code;

  Integer creditNumber;

  Integer status;

  String sectionName;

  Integer sectionId;
}
