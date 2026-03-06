package com.bachld.backend.controller;

import com.bachld.backend.dto.request.LoginRequest;
import com.bachld.backend.dto.request.UserCreateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;

    @PostMapping("v1/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), authService.login(loginRequest)));
    }

    @PostMapping("v1/register")
    public ResponseEntity<?> login(@RequestBody @Valid UserCreateRequest request) {
        authService.register(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }
}
