package com.bachld.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportVeyonKeyRequest {

    @NotNull(message = "ID lớp học không được để trống")
    Integer classId;

    @NotBlank(message = "Tên khóa không được để trống")
    String keyName;

    @NotBlank(message = "Dữ liệu khóa mã hóa không được để trống")
    String encryptedKeyData;
}