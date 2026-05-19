package com.bachld.backend.dto.request;

import jakarta.validation.constraints.Min;
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
public class ExamRoomUpdateRequest {

    String code;

    Integer roomId;

    Integer teacher1Id;

    Integer teacher2Id;

    Integer subjectId;

    Integer semesterId;

    @Min(value = 1, message = "Sĩ số tối đa phải lớn hơn 0")
    Integer maxStudent;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Ngày thi phải có định dạng yyyy-MM-dd")
    String examDate;

    @Pattern(regexp = "^\\d{2}:\\d{2}:\\d{2}$", message = "Giờ bắt đầu phải có định dạng HH:mm")
    String startTime;

    @Pattern(regexp = "^\\d{2}:\\d{2}:\\d{2}$", message = "Giờ kết thúc phải có định dạng HH:mm")
    String endTime;
}