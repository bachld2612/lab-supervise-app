package com.bachld.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LockScreenExamRoomRequest {

  @NotNull(message = "ID phòng thi không được để trống") Integer examRoomId;

  @NotNull(message = "ID user của sinh viên không được để trống") Integer studentUserId;

  @NotNull(message = "Trạng thái khóa màn hình không được để trống") Boolean active;
}
