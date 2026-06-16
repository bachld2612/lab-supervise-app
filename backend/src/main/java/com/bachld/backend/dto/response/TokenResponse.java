package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Body returned by the refresh endpoint: a freshly minted access token. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TokenResponse {

    String token;
}
