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
@Table(name = "incident_reports")
public class IncidentReport extends BaseEntity {

  String title;

  @Column(name = "room_id")
  Integer roomId;

  @Column(name = "reporter_id")
  Integer reporterId;

  @Column(name = "reporter_role", length = 20)
  String reporterRole;

  @Column(name = "handler_id")
  Integer handlerId;
}
