package com.bachld.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
@MappedSuperclass
@NoArgsConstructor
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    int status;

    @Column(name = "created_at")
    @JsonIgnore
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "created_user")
    @JsonIgnore
    @CreatedBy
    String createdUser;

    @Column(name = "updated_at")
    @JsonIgnore
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @Column(name = "updated_user")
    @JsonIgnore
    @LastModifiedBy
    String updatedUser;
}