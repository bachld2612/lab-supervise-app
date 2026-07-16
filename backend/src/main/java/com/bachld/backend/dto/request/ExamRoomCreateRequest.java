package com.bachld.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class ExamRoomCreateRequest {

  @NotEmpty(message = "Mã phòng thi không được để trống") String code;

  @NotNull(message = "Phòng học không được để trống") Integer roomId;

  @NotNull(message = "Giảng viên coi thi 1 không được để trống") Integer teacher1Id;

  @NotNull(message = "Giảng viên coi thi 2 không được để trống") Integer teacher2Id;

  @NotNull(message = "Môn thi không được để trống") Integer subjectId;

  @NotNull(message = "Học kỳ không được để trống") Integer semesterId;

  @NotNull(message = "Sĩ số tối đa không được để trống") @Min(value = 1, message = "Sĩ số tối đa phải lớn hơn 0") Integer maxStudent;

  @NotEmpty(message = "Ngày thi không được để trống") @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Ngày thi phải có định dạng yyyy-MM-dd") String examDate;

  @NotEmpty(message = "Tiết thi không được để trống") String periods;

  @Pattern(
      regexp = "^\\d{2}:\\d{2}(:\\d{2})?$",
      message = "Giờ bắt đầu phải có định dạng HH:mm hoặc HH:mm:ss")
  String startTime;

  @Pattern(
      regexp = "^\\d{2}:\\d{2}(:\\d{2})?$",
      message = "Giờ kết thúc phải có định dạng HH:mm hoặc HH:mm:ss")
  String endTime;
}
