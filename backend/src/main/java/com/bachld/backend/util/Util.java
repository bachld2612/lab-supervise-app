package com.bachld.backend.util;

import com.bachld.backend.model.Role;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.RoleRepository;
import com.bachld.backend.repository.UserRepository;
import com.bachld.backend.util.enums.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Util {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    public User getCurrentUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByIdAndStatus(Integer.valueOf(userId), Status.ACTIVE.getValue()).orElse(null);
    }

    public void validateEmail(String email, Integer userId) {
        Optional<User> user = userRepository.findByEmailAndStatus(email, Status.ACTIVE.getValue());
        if (user.isPresent()) {
            if (userId == null || user.get().getId() != userId) {
                throw new IllegalArgumentException("Email đã tồn tại");
            }
        }
    }

    public void validatePhone(String phone, Integer userId) {
        Optional<User> user = userRepository.findByPhoneAndStatus(phone, Status.ACTIVE.getValue());
        if (user.isPresent()) {
            if (userId == null || user.get().getId() != userId) {
                throw new IllegalArgumentException("Số điện thoại đã tồn tại");
            }
        }
    }

    public void validateRole(Integer roleId) {
        Optional<Role> role = roleRepository.findById(roleId);
        if (role.isEmpty()) {
            throw new IllegalArgumentException("Role không hợp lệ");
        }
    }
}
