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
@Table(name = "student_exam_room_info")
public class StudentExamRoomInfo extends BaseEntity {

  @Column(name = "student_exam_room_id")
  Integer studentExamRoomId;

  @Column(name = "application_name", columnDefinition = "TEXT")
  String applicationName;

  @Column(name = "is_violation", columnDefinition = "BOOLEAN DEFAULT FALSE")
  boolean violation;

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
