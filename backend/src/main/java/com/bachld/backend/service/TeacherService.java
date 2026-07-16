package com.bachld.backend.service;

import com.bachld.backend.dto.request.TeacherCreateRequest;
import com.bachld.backend.dto.request.TeacherUpdateRequest;
import com.bachld.backend.dto.response.TeacherResponse;
import com.bachld.backend.model.Teacher;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.TeacherRepository;
import com.bachld.backend.repository.UserRepository;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Role;
import com.bachld.backend.util.enums.Status;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeacherService {

  TeacherRepository teacherRepository;

  UserRepository userRepository;

  PasswordEncoder passwordEncoder;

  Util util;

  public Page<TeacherResponse> getList(Pageable pageable, String keyword, Integer status) {
    if (keyword != null) {
      keyword = "%" + keyword.trim().toLowerCase() + "%";
    } else {
      keyword = "%%";
    }

    return teacherRepository.findByKeyword(pageable, keyword, status);
  }

  public TeacherResponse getById(Integer id) {
    return teacherRepository.findTeacherByIdAndStatus(id, Status.ACTIVE.getValue());
  }

  @Transactional
  public void create(TeacherCreateRequest request) {
    util.validatePhone(request.getPhone(), null);
    util.validateEmail(request.getEmail(), null);
    util.validateTeacherCode(request.getCode(), null);
    util.validateSection(request.getSectionId());

    User user = new User();
    user.setEmail(request.getEmail());
    String rawPassword = "tlu" + request.getPhone().substring(request.getPhone().length() - 3);
    user.setPassword(passwordEncoder.encode(rawPassword));
    user.setRawPassword(rawPassword);
    user.setFullName(request.getFullName());
    user.setHometown(request.getHometown());
    user.setBirthday(LocalDate.parse(request.getBirthday()));
    user.setPhone(request.getPhone());
    user.setRoleId(Role.TEACHER.getValue());
    user.setStatus(Status.ACTIVE.getValue());

    userRepository.save(user);

    Teacher teacher = new Teacher();
    teacher.setCode(request.getCode());
    teacher.setUserId(user.getId());
    teacher.setSectionId(request.getSectionId());
    teacher.setStatus(Status.ACTIVE.getValue());
    teacherRepository.save(teacher);
  }

  @Transactional
  public void update(TeacherUpdateRequest request, int id) {
    Teacher teacher =
        teacherRepository
            .findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy giáo viên có id: " + id));

    User user =
        userRepository
            .findByIdAndStatus(teacher.getUserId(), Status.ACTIVE.getValue())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Không tìm thấy người dùng có id: " + teacher.getUserId()));

    util.validatePhone(request.getPhone(), user.getId());
    util.validateEmail(request.getEmail(), user.getId());
    util.validateTeacherCode(request.getCode(), teacher.getId());

    if (request.getEmail() != null && !request.getEmail().isEmpty()) {
      user.setEmail(request.getEmail());
    }

    if (request.getCode() != null && !request.getCode().isEmpty()) {
      teacher.setCode(request.getCode());
    }

    if (request.getFullName() != null && !request.getFullName().isEmpty()) {
      user.setFullName(request.getFullName());
    }

    if (request.getHometown() != null && !request.getHometown().isEmpty()) {
      user.setHometown(request.getHometown());
    }

    if (request.getSectionId() != null) {
      util.validateSection(request.getSectionId());
      teacher.setSectionId(request.getSectionId());
    }

    if (request.getBirthday() != null && !request.getBirthday().isEmpty()) {
      user.setBirthday(LocalDate.parse(request.getBirthday()));
    }

    if (request.getPhone() != null && !request.getPhone().isEmpty()) {
      user.setPhone(request.getPhone());
    }

    userRepository.save(user);
    teacherRepository.save(teacher);
  }

  public void deleteById(Integer id) {
    Teacher teacher =
        teacherRepository
            .findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy giảng viên có id: " + id));

    User user =
        userRepository
            .findByIdAndStatus(teacher.getUserId(), Status.ACTIVE.getValue())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Người dùng có id: "
                            + teacher.getUserId()
                            + "không tồn tại hoặc đã bị xoá"));
    user.setStatus(Status.INACTIVE.getValue());

    userRepository.save(user);
  }
}
