package com.bachld.backend.service;

import com.bachld.backend.dto.request.ChangePasswordRequest;
import com.bachld.backend.dto.request.UserCreateRequest;
import com.bachld.backend.dto.request.UserUpdateRequest;
import com.bachld.backend.dto.response.UserResponse;
import com.bachld.backend.model.Student;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.StudentRepository;
import com.bachld.backend.repository.UserRepository;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Role;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    PasswordEncoder passwordEncoder;

    UserRepository userRepository;

    StudentRepository studentRepository;

    Util util;

    public UserResponse getProfile() {
        var userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findUserByIdAndStatus(Integer.parseInt(userId), Status.ACTIVE.getValue());
    }

    @Transactional
    public void create(UserCreateRequest request) {
        util.validatePhone(request.getPhone(), null);
        util.validateEmail(request.getEmail(), null);
        util.validateRole(request.getRoleId());

        User user = new User();
        user.setEmail(request.getEmail());
        String rawPassword = "tlu" + request.getPhone().substring(request.getPhone().length() - 3);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRawPassword(rawPassword);
        user.setFullName(request.getFullName());
        user.setHometown(request.getHometown());
        user.setBirthday(LocalDate.parse(request.getBirthday()));
        user.setPhone(request.getPhone());
        user.setRoleId(request.getRoleId());
        user.setStatus(Status.ACTIVE.getValue());

        userRepository.save(user);
    }

    @Transactional
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

        if (request.getHometown() != null && !request.getHometown().isEmpty()) {
            user.setHometown(request.getHometown());
        }

        if (request.getBirthday() != null && !request.getBirthday().isEmpty()) {
            user.setBirthday(LocalDate.parse(request.getBirthday()));
        }

        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            user.setPhone(request.getPhone());
        }

        if (request.getRoleId() != null) {
            user.setRoleId(request.getRoleId());
        }

        userRepository.save(user);
    }

    public Page<UserResponse> getList(Pageable pageable, String keyword, Integer status, Integer roleType, Integer roleId) {
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        }
        else {
            keyword = "%%";
        }

        return userRepository.findAllByKeyword(pageable, keyword, status, roleType, roleId);
    }

    public UserResponse getById(int id) {
        return userRepository.findUserByIdAndStatus(id, Status.ACTIVE.getValue());
    }

    public void deleteById(int id) {
        User user = userRepository.findByIdAndStatus(id, Status.ACTIVE.getValue())
                .orElseThrow(() -> new IllegalArgumentException("Người dùng đã bị xoá hoặc không tồn tại"));
        user.setStatus(Status.INACTIVE.getValue());
        userRepository.save(user);
    }

    public void changePassword(ChangePasswordRequest request) {
        User currentUser = util.getCurrentUser();

        if (!passwordEncoder.matches(request.getOldPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác");
        }

        if (Objects.equals(currentUser.getRawPassword(), request.getNewPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được giống mật khẩu mặc định");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        currentUser.setRawPassword(null);
        userRepository.save(currentUser);
    }

    public void resetPassword(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có id: " + userId));

        String rawPassword = buildDefaultPassword(user);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRawPassword(rawPassword);

        userRepository.save(user);
    }

    private String buildDefaultPassword(User user) {
        if (user.getRoleId() != null && user.getRoleId() == Role.STUDENT.getValue()) {
            Student student = studentRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên của người dùng có id: " + user.getId()));
            return buildStudentDefaultPassword(student.getCode());
        }

        return "tlu" + user.getPhone().substring(user.getPhone().length() - 3);
    }

    private String buildStudentDefaultPassword(String studentCode) {
        String normalizedCode = studentCode.trim();
        if (normalizedCode.length() < 3) {
            throw new IllegalArgumentException("Mã sinh viên phải có ít nhất 3 ký tự để tạo mật khẩu mặc định");
        }
        return "tlu" + normalizedCode.substring(normalizedCode.length() - 3);
    }
}
