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
@Table(name = "allowed_application")
public class AllowedApplication extends BaseEntity {

  @Column(name = "exam_room_id")
  Integer examRoomId;

  @Column(name = "application_name", columnDefinition = "TEXT")
  String applicationName;

  @Column(name = "image_url", columnDefinition = "TEXT")
  String imageUrl;
}
