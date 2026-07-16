package com.bachld.backend.model;

import jakarta.persistence.Column;
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
@Table(name = "student_exam_room")
public class StudentExamRoom extends BaseEntity {

  @Column(name = "exam_room_id")
  Integer examRoomId;

  @Column(name = "student_id")
  Integer studentId;
}
