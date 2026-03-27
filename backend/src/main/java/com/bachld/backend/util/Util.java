package com.bachld.backend.util;

import com.bachld.backend.model.Role;
import com.bachld.backend.model.Student;
import com.bachld.backend.model.Teacher;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.RoleRepository;
import com.bachld.backend.repository.StudentRepository;
import com.bachld.backend.repository.TeacherRepository;
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

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

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

    public void validateTeacherCode(String code, Integer teacherId) {
        Optional<Teacher> teacher = teacherRepository.findByCodeAndStatus(code, Status.ACTIVE.getValue());
        if (teacher.isPresent()) {
            if (teacherId == null || teacher.get().getId() != teacherId) {
                throw new IllegalArgumentException("Mã giảng viên đã tồn tại");
            }
        }
    }

    public void validateStudentCode(String code, Integer studentId) {
        Optional<Student> student = studentRepository.findByCodeAndStatus(code, Status.ACTIVE.getValue());
        if (student.isPresent()) {
            if (studentId == null || student.get().getId() != studentId) {
                throw new IllegalArgumentException("Mã sinh viên đã tồn tại");
            }
        }
    }
}
