package com.bachld.backend.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SemesterUpdateRequest {

  String name;

  @Pattern(
      regexp = "^(\\d{4}\\s-\\s\\d{4})?$",
      message = "Năm học phải có định dạng: năm bắt đầu - năm kết thúc")
  String studyYear;

  @Pattern(
      regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$",
      message = "Ngày bắt đầu phải có định dạng yyyy-MM-dd")
  String startDate;

  @Pattern(
      regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$",
      message = "Ngày kết thúc phải có định dạng yyyy-MM-dd")
  String endDate;
}
