package com.bachld.backend.service;

import com.bachld.backend.dto.request.LoginRequest;
import com.bachld.backend.dto.request.UserCreateRequest;
import com.bachld.backend.dto.response.LoginResponse;
import com.bachld.backend.dto.response.RoleResponse;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.RoleRepository;
import com.bachld.backend.repository.UserRepository;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    UserRepository userRepository;

    AuthenticationManager authenticationManager;

    RoleRepository roleRepository;

    JwtService jwtService;

    PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác"));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getId(), loginRequest.getPassword())
        );

        if (authentication.isAuthenticated()) {
            RoleResponse roleResponse = roleRepository.findRoleById(user.getRoleId());
            String token = jwtService.generateToken(String.valueOf(user.getId()));

            return LoginResponse.builder()
                    .token(token)
                    .user(user)
                    .role(roleResponse)
                    .build();
        }
        throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác");
    }

    public void register(UserCreateRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setStatus(Status.ACTIVE.getValue());
        userRepository.save(user);
    }
}
