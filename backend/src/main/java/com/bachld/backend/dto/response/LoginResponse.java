package com.bachld.backend.dto.response;

import com.bachld.backend.model.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginResponse {

    String token;

    User user;

    RoleResponse role;
}