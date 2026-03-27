package com.bachld.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {

    String email;

    @JsonIgnore
    String password;

    @Column(name = "raw_password")
    String rawPassword;

    @Column(name = "full_name")
    String fullName;

    String phone;

    @Column(name = "hometown")
    String hometown;

    @Column(name = "birthday")
    LocalDate birthday;

    @Column(name = "role_id")
    Integer roleId;
}
