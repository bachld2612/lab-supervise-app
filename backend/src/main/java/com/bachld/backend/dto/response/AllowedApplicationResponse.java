package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AllowedApplicationResponse {

    Integer id;

    Integer examRoomId;

    String applicationName;

    String imageUrl;

    Integer status;
}