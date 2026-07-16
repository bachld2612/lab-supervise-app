package com.bachld.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomCreateRequest {

  @NotEmpty(message = "Tên phòng không được phép bỏ trống") String name;

  @NotNull(message = "Số lượng chỗ ngồi không được phép bỏ trống") @Min(value = 1, message = "Số lượng chỗ ngồi phải lớn hơn 0") Integer capacity;
}
