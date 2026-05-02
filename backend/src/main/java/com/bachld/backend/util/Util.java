package com.bachld.backend.util;

import com.bachld.backend.dto.response.SectionResponse;
import com.bachld.backend.dto.response.SubjectResponse;
import com.bachld.backend.model.*;
import com.bachld.backend.repository.*;
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

    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private PersonalComputerRepository personalComputerRepository;

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
        if (role.isEmpty() || role.get().getStatus() == Status.INACTIVE.getValue()) {
            throw new IllegalArgumentException("Role không hợp lệ");
        }
    }

    public void validateSection(Integer sectionId) {
        SectionResponse section = sectionRepository.findByIdAndStatus(sectionId, Status.ACTIVE.getValue());
        if (section == null) {
            throw new IllegalArgumentException("Bộ môn không hợp lệ");
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

    public void validateSubjectCode(String code, Integer id) {
        Optional<Subject> subject = subjectRepository.findByCodeAndStatus(code, Status.ACTIVE.getValue());
        if (subject.isPresent()) {
            if (id == null || subject.get().getId() != id) {
                throw new IllegalArgumentException("Mã môn học đã tồn tại");
            }
        }
    }

    public void validateIpAddress(String ipAddress, Integer userId, Integer roleId) {
        Optional<PersonalComputer> pc = personalComputerRepository.findByIpAddressAndRoleId(ipAddress, roleId);
        
        if (pc.isPresent()) {
            if (userId == null || !pc.get().getUserId().equals(userId)) {
                throw new IllegalArgumentException("Địa chỉ IP đã tồn tại ở thiết bị khác");
            }
        }
    }
}
