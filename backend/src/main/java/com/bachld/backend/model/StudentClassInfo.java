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
@Table(name = "student_class_info")
public class StudentClassInfo extends BaseEntity {

    @Column(name = "student_class_id")
    Integer studentClassId;

    @Column(name = "application_name")
    String applicationName;

    @Column(name = "is_ban_application", columnDefinition = "BOOLEAN DEFAULT FALSE")
    boolean isBanApplication;
}
