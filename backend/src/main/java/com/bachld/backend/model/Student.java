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
@Table(name = "students")
public class Student extends BaseEntity {

  String code;

  @Column(name = "manage_class_id")
  Integer manageClassId;

  @Column(name = "user_id")
  Integer userId;
}
