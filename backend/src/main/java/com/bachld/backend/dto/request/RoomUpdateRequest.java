package com.bachld.backend.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomUpdateRequest {

    String name;

    @Min(value = 1, message = "Số lượng chỗ ngồi phải lớn hơn 0")
    Integer capacity;
}