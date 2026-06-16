package com.bachld.backend.service;

import com.bachld.backend.dto.request.LoginRequest;
import com.bachld.backend.dto.response.AuthResult;
import com.bachld.backend.dto.response.LoginResponse;
import com.bachld.backend.dto.response.RoleResponse;
import com.bachld.backend.model.RefreshToken;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.ClassRepository;
import com.bachld.backend.repository.ExamRoomRepository;
import com.bachld.backend.repository.RefreshTokenRepository;
import com.bachld.backend.repository.RoleRepository;
import com.bachld.backend.repository.UserRepository;
import com.bachld.backend.util.enums.Status;
import lombok.experimental.NonFinal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    static final Logger log = LoggerFactory.getLogger(AuthService.class);

    UserRepository userRepository;

    AuthenticationManager authenticationManager;

    RoleRepository roleRepository;

    JwtService jwtService;

    ClassRepository classRepository;

    ExamRoomRepository examRoomRepository;

    RefreshTokenRepository refreshTokenRepository;

    TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.refresh-expire-day}")
    @NonFinal
    int REFRESH_TOKEN_EXPIRED_DAY;

    public AuthResult login(LoginRequest loginRequest) {
        User user = userRepository.findByEmailAndStatus(loginRequest.getEmail(), Status.ACTIVE.getValue())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác"));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getId(), loginRequest.getPassword())
        );

        if (authentication.isAuthenticated()) {
            if (Objects.equals(loginRequest.getDevice(), "web") && user.getRoleId() == 3) {
                throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác");
            }

            if (Objects.equals(loginRequest.getDevice(), "desktop") && user.getRoleId() != 3) {
                throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác");
            }

            if (Objects.equals(loginRequest.getDevice(), "desktop")) {
                LocalDate today = LocalDate.now();
                boolean hasActiveSchedule =
                        classRepository.countActiveClassesToday(user.getId(), today) > 0
                        || examRoomRepository.countActiveExamsTodayByUserId(user.getId(), today) > 0;
                if (!hasActiveSchedule) {
                    throw new IllegalArgumentException("Xác minh vị trí không thành công. Vui lòng đảm bảo bạn đang trong khu vực lớp học.");
                }

                List<String> validSsids = classRepository.findActiveWifiSsidsByStudentUserId(user.getId(), today);
                String sent = loginRequest.getWifiSsid();
                if (validSsids.isEmpty() || sent == null || sent.isBlank() || !validSsids.contains(sent)) {
                    throw new IllegalArgumentException("Xác minh vị trí không thành công. Vui lòng đảm bảo bạn đang trong khu vực lớp học.");
                }
            }

            RoleResponse roleResponse = roleRepository.findRoleById(user.getRoleId());
            String token = jwtService.generateToken(String.valueOf(user.getId()));
            String refreshToken = createRefreshToken(user.getId());

            LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .user(user)
                    .role(roleResponse)
                    .build();
            return new AuthResult(response, refreshToken);
        }
        throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác");
    }

    /** Create and persist a new opaque (UUID) refresh token for the user. */
    private String createRefreshToken(Integer userId) {
        String token = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.builder()
                .token(token)
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRED_DAY))
                .build());
        return token;
    }

    /**
     * Exchange a valid, non-expired refresh token for a new access token.
     * Throws if the token is unknown or expired (expired tokens are deleted).
     */
    public String refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Phiên đăng nhập không hợp lệ");
        }
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Phiên đăng nhập không hợp lệ"));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.deleteByToken(refreshToken);
            throw new IllegalArgumentException("Phiên đăng nhập đã hết hạn");
        }
        return jwtService.generateToken(String.valueOf(stored.getUserId()));
    }

    /**
     * Logout: delete the refresh token from DB and blacklist the access token
     * (by jti) until its natural expiry. Both arguments are best-effort.
     */
    public void logout(String refreshToken, String accessToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.deleteByToken(refreshToken);
        }
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                String jti = jwtService.extractJti(accessToken);
                Date exp = jwtService.extractExpiration(accessToken);
                tokenBlacklistService.blacklist(jti, exp.getTime());
            } catch (Exception e) {
                log.debug("Could not blacklist access token on logout: {}", e.getMessage());
            }
        }
    }

    public List<String> getValidWifiSsids(String email) {
        var userOpt = userRepository.findByEmailAndStatus(email, Status.ACTIVE.getValue());
        if (userOpt.isEmpty()) {
            return List.of();
        }
        User user = userOpt.get();
        return classRepository.findActiveWifiSsidsByStudentUserId(user.getId(), LocalDate.now());
    }
}
