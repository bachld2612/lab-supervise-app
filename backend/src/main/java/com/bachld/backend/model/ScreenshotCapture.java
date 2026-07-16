package com.bachld.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@Getter
@Setter
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Table(name = "screenshot_capture")
public class ScreenshotCapture extends BaseEntity {

  @Column(name = "student_class_id")
  Integer studentClassId;

  @Column(name = "student_exam_room_id")
  Integer studentExamRoomId;

  @Column(name = "image_path", columnDefinition = "TEXT")
  String imagePath;
}
