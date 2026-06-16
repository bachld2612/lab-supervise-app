package com.bachld.backend.controller;

import com.bachld.backend.dto.request.LoginRequest;
import com.bachld.backend.dto.response.AuthResult;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.dto.response.TokenResponse;
import com.bachld.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    static final String REFRESH_COOKIE = "refreshToken";
    static final String COOKIE_PATH = "/api/auth";

    AuthService authService;

    @Value("${jwt.refresh-expire-day}")
    @NonFinal
    int REFRESH_TOKEN_EXPIRED_DAY;

    @Value("${auth.cookie-secure}")
    @NonFinal
    boolean COOKIE_SECURE;

    @PostMapping("v1/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest) {
        AuthResult result = authService.login(loginRequest);
        ResponseCookie cookie = buildRefreshCookie(result.getRefreshToken(), Duration.ofDays(REFRESH_TOKEN_EXPIRED_DAY));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new BaseResponse<>(HttpStatus.OK.value(), result.getResponse()));
    }

    @PostMapping("v1/refresh")
    public ResponseEntity<?> refresh(@CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
        String accessToken = authService.refresh(refreshToken);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), new TokenResponse(accessToken)));
    }

    @PostMapping("v1/logout")
    public ResponseEntity<?> logout(@CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
                                    HttpServletRequest request) {
        authService.logout(refreshToken, extractAccessToken(request));
        ResponseCookie cleared = buildRefreshCookie("", Duration.ZERO);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleared.toString())
                .body(new BaseResponse<>(HttpStatus.OK.value(), "Đăng xuất thành công"));
    }

    @GetMapping("v1/wifi-ssid")
    public ResponseEntity<?> getValidWifiSsids(@RequestParam String email) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), authService.getValidWifiSsids(email)));
    }

    private ResponseCookie buildRefreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(COOKIE_SECURE)
                .path(COOKIE_PATH)
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
    }

    private String extractAccessToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
