package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BanApplicationResponse {

  Integer id;

  Integer teacherId;

  String applicationName;

  String imageUrl;

  Integer status;
}
