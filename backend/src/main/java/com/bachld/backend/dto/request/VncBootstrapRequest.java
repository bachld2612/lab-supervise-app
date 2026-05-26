package com.bachld.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Desktop app gửi lên sau khi installer cài UltraVNC xong và sinh ra
 * password cho máy. Password được desktop encrypt rồi mới gửi.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VncBootstrapRequest {

    @NotEmpty(message = "Password không được phép bỏ trống")
    String vncPassword;
}
