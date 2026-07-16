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
@Table(name = "teachers")
public class Teacher extends BaseEntity {

  String code;

  @Column(name = "section_id")
  Integer sectionId;

  @Column(name = "user_id")
  Integer userId;
}
