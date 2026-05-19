package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WhitelistUpdateMessage {

    String type = "WHITELIST_UPDATE";
    Integer examRoomId;
    List<AllowedApplicationResponse> allowedApplications;
}