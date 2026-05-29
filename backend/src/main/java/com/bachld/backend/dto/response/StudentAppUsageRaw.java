package com.bachld.backend.dto.response;

import com.bachld.backend.util.enums.TrackingAction;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentAppUsageRaw {

    Integer studentId;

    String applicationName;

    TrackingAction action;

    String clipboardTextEncrypted;

    String clipboardKeyEncrypted;

    String clipboardIv;

    LocalDateTime createdAt;

    boolean isBanApplication;

    String connectionType;

    public StudentAppUsageRaw(Integer studentId, String applicationName, LocalDateTime createdAt,
                              boolean isBanApplication, String connectionType) {
        this(studentId, applicationName, TrackingAction.NORMAL, null, null, null, createdAt, isBanApplication, connectionType);
    }
}
