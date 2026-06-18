package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
