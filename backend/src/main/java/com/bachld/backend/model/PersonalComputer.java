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
@Table(name = "personal_computer")
public class PersonalComputer extends BaseEntity {

    @Column(name = "ip_address")
    String ipAddress;

    @Column(name = "user_id")
    Integer userId;

    @Column(name = "vnc_password_encrypted", length = 512)
    String vncPasswordEncrypted;
}
