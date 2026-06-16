package com.bachld.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Internal result of a successful authentication: the response body returned to
 * the client plus the opaque refresh-token UUID that the controller writes into
 * an HttpOnly cookie (never into the body).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthResult {

    LoginResponse response;

    String refreshToken;
}
