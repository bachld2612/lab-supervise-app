package com.bachld.backend.model;

import com.bachld.backend.model.converter.TrackingActionConverter;
import com.bachld.backend.util.enums.TrackingAction;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@Getter
@Setter
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Table(name = "student_class_info")
public class StudentClassInfo extends BaseEntity {

    @Column(name = "student_class_id")
    Integer studentClassId;

    @Column(name = "application_name", columnDefinition = "TEXT")
    String applicationName;

    @Column(name = "is_ban_application", columnDefinition = "BOOLEAN DEFAULT FALSE")
    boolean isBanApplication;

    @Column(name = "connection_type", length = 20)
    String connectionType;

    @Convert(converter = TrackingActionConverter.class)
    @Column(name = "action", columnDefinition = "INT DEFAULT 0")
    TrackingAction action = TrackingAction.NORMAL;

    @Column(name = "clipboard_text_encrypted", columnDefinition = "TEXT")
    String clipboardTextEncrypted;

    @Column(name = "clipboard_key_encrypted", columnDefinition = "TEXT")
    String clipboardKeyEncrypted;

    @Column(name = "clipboard_iv", columnDefinition = "TEXT")
    String clipboardIv;
}
