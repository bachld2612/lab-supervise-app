package com.bachld.backend.util.enums;

import java.time.LocalTime;
import lombok.Getter;

@Getter
public enum Period {
  P1(1, "07:00", "07:50"),
  P2(2, "07:55", "08:45"),
  P3(3, "08:50", "09:40"),
  P4(4, "09:45", "10:35"),
  P5(5, "10:40", "11:30"),
  P6(6, "11:35", "12:25"),
  P7(7, "12:55", "13:45"),
  P8(8, "13:50", "14:40"),
  P9(9, "14:45", "15:35"),
  P10(10, "15:40", "16:30"),
  P11(11, "16:35", "17:25"),
  P12(12, "17:30", "18:20");

  private final int value;
  private final LocalTime startTime;
  private final LocalTime endTime;

  Period(int value, String startTime, String endTime) {
    this.value = value;
    this.startTime = LocalTime.parse(startTime);
    this.endTime = LocalTime.parse(endTime);
  }

  public static Period fromValue(int value) {
    for (Period period : Period.values()) {
      if (period.value == value) {
        return period;
      }
    }
    throw new IllegalArgumentException("Tiết học không hợp lệ: " + value);
  }
}
