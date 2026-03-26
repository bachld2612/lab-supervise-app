package com.bachld.backend.service;

import com.bachld.backend.dto.request.UserCreateRequest;
import com.bachld.backend.dto.request.UserUpdateRequest;
import com.bachld.backend.dto.response.UserResponse;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.RoleRepository;
import com.bachld.backend.repository.UserRepository;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    PasswordEncoder passwordEncoder;

    UserRepository userRepository;
    private final Util util;
    private final RoleRepository roleRepository;

    public void create(UserCreateRequest request) {
        util.validatePhone(request.getPhone(), null);
        util.validateEmail(request.getEmail(), null);
        util.validateRole(request.getRoleId());

        User user = new User();
        user.setEmail(request.getEmail());
        String rawPassword = request.getPhone().substring(0, request.getPhone().length() - 3);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRawPassword(rawPassword);
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRoleId(request.getRoleId());
        user.setStatus(Status.ACTIVE.getValue());

        userRepository.save(user);
    }

    public void update(UserUpdateRequest request, int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có id: " + userId));

        util.validateEmail(request.getEmail(), user.getId());
        util.validatePhone(request.getPhone(), user.getId());
        util.validateRole(request.getRoleId());

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            user.setEmail(request.getEmail());
        }

        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setFullName(request.getFullName());
        }

        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            user.setPhone(request.getPhone());
        }

        if (request.getRoleId() != null) {
            user.setRoleId(request.getRoleId());
        }

        userRepository.save(user);
    }

    public Page<UserResponse> getList(Pageable pageable, String keyword, Integer status, Integer roleType) {
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        }
        else {
            keyword = "%%";
        }

        return userRepository.findAllByKeyword(pageable, keyword, status, roleType);
    }

    public UserResponse getById(int id) {
        return userRepository.findUserByIdAndStatus(id, Status.ACTIVE.getValue());
    }
}
