package com.bachld.backend.util.enums;

import lombok.Getter;

@Getter
public enum RoleType {

    STAFF(1), TEACHER(2), STUDENT(3);

    private final int value;

    RoleType(int value) {
        this.value = value;
    }
}
