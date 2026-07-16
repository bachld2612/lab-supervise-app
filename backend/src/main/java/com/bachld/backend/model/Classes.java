package com.bachld.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@Getter
@Setter
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Table(name = "classes")
public class Classes extends BaseEntity {

  String name;

  @Column(name = "max_student")
  Integer maxStudent;

  @Column(name = "session_number")
  Integer sessionNumber;

  @Column(name = "subject_id")
  Integer subjectId;

  @Column(name = "teacher_id")
  Integer teacherId;

  @Column(name = "schedule_id")
  Integer scheduleId;

  @Column(name = "semester_id")
  Integer semesterId;

  @Column(name = "start_date")
  LocalDate startDate;

  @Column(name = "end_date")
  LocalDate endDate;

  @Column(name = "room_id")
  Integer roomId;

  @Column(name = "wifi_ssid")
  String wifiSsid;

  @Column(name = "tracking_enabled", columnDefinition = "BOOLEAN DEFAULT TRUE")
  Boolean trackingEnabled = true;
}
