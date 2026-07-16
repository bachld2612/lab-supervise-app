package com.bachld.backend.util.enums;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum TrackingAction {
  NORMAL(0),
  COPY(1),
  PASTE(2),
  CUT(3);

  private final int value;

  TrackingAction(int value) {
    this.value = value;
  }

  public static TrackingAction fromValue(Integer value) {
    if (value == null) {
      return NORMAL;
    }
    return Arrays.stream(values())
        .filter(action -> action.value == value)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported tracking action: " + value));
  }
}
