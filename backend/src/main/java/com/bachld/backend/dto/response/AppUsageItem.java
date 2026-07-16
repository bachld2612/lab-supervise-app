package com.bachld.backend.dto.response;

import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppUsageItem {

  String applicationName;

  Integer action;

  String clipboardText;

  LocalDateTime createdAt;

  boolean isBanApplication;

  String connectionType;
}
