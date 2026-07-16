package com.bachld.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class AllowedApplicationCreateRequest {

  @NotNull(message = "Phòng thi không được để trống") Integer examRoomId;

  @NotBlank(message = "Tên ứng dụng không được để trống") String applicationName;

  String imageUrl;
}
