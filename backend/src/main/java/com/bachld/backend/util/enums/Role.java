package com.bachld.backend.util.enums;

import lombok.Getter;

@Getter
public enum Role {
  ADMIN(1),
  TEACHER(2),
  STUDENT(3),
  IT_CENTER(4);

  private final int value;

  Role(int value) {
    this.value = value;
  }
}
