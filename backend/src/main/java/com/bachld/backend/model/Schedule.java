package com.bachld.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@Getter
@Setter
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Table(name = "schedules")
public class Schedule extends BaseEntity {

  String name;

  @Column(name = "days_of_week")
  String daysOfWeek;

  @Column(name = "periods")
  String periods;

  @Column(name = "start_time")
  LocalTime startTime;

  @Column(name = "end_time")
  LocalTime endTime;
}
