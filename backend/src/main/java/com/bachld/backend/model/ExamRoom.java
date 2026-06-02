package com.bachld.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@Getter
@Setter
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Table(name = "exam_room")
public class ExamRoom extends BaseEntity {

    @Column(unique = true)
    String code;

    @Column(name = "room_id")
    Integer roomId;

    @Column(name = "teacher1_id")
    Integer teacher1Id;

    @Column(name = "teacher2_id")
    Integer teacher2Id;

    @Column(name = "subject_id")
    Integer subjectId;

    @Column(name = "semester_id")
    Integer semesterId;

    @Column(name = "max_student")
    Integer maxStudent;

    @Column(name = "exam_date")
    LocalDate examDate;

    @Column(name = "periods")
    String periods;

    @Column(name = "start_time")
    LocalTime startTime;

    @Column(name = "end_time")
    LocalTime endTime;

    @Column(name = "tracking_enabled")
    Boolean trackingEnabled = false;

    @Column(name = "wifi_ssid")
    String wifiSsid;
}
