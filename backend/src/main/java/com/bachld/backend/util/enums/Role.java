package com.bachld.backend.util.enums;

import lombok.Getter;

@Getter
public enum Role {
    ADMIN(0), TEACHER(1), STUDENT(2);
    ;

    private final int value;

    Role(int value) {
        this.value = value;
    }

}
