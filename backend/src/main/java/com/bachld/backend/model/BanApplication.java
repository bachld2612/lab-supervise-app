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
@Table(name = "ban_application")
public class BanApplication extends BaseEntity {

    @Column(name = "teacher_id")
    Integer teacherId;

    @Column(name = "application_name", columnDefinition = "TEXT")
    String applicationName;

    @Column(name = "image_url", columnDefinition = "TEXT")
    String imageUrl;
}