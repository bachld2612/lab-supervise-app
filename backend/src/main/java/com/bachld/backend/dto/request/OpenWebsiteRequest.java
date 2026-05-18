package com.bachld.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OpenWebsiteRequest {

    @NotBlank(message = "URL website không được để trống")
    String websiteUrl;
}
