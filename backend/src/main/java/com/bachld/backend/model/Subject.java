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
@Table(name = "subjects")
public class Subject extends BaseEntity {

    String name;

    String code;

    @Column(name = "credit_number")
    Integer creditNumber;

    @Column(name = "section_id")
    Integer sectionId;
}
