package com.bachld.backend.dto.response;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WhitelistUpdateMessage {

  String type = "WHITELIST_UPDATE";

  Integer examRoomId;

  List<AllowedApplicationResponse> allowedApplications;
}
