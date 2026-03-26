package com.bachld.backend.config;

import com.bachld.backend.model.User;
import com.bachld.backend.repository.UserRepository;
import com.bachld.backend.util.enums.Role;
import com.bachld.backend.util.enums.Status;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DataConfig {

    UserRepository userRepository;

    PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        User admin = userRepository.findByEmailAndStatus("vpk@tlu.edu.vn", Status.ACTIVE.getValue())
                .orElse(null);
        if (admin == null) {
            User user = new User();
            user.setEmail("vpk@tlu.edu.vn");
            user.setFullName("Văn phòng khoa");
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRoleId(Role.ADMIN.getValue());
            user.setPhone("0123456789");
            user.setStatus(Status.ACTIVE.getValue());
            userRepository.save(user);
        }
    }
}
