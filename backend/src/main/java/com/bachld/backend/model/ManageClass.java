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
@Table(name = "manage_class")
public class ManageClass extends BaseEntity {

  String name;

  @Column(name = "max_student")
  Integer maxStudent;

  @Column(name = "teacher_id")
  Integer teacherId;

  @Column(name = "major_id")
  Integer majorId;
}
