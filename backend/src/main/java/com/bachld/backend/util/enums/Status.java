package com.bachld.backend.util.enums;

public enum Status {

    INACTIVE(0), ACTIVE(1), LOCKED(2);
    private final int value;

    Status(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}