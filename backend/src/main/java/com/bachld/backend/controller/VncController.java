package com.bachld.backend.controller;

import com.bachld.backend.config.ConnectedExamStudentRegistry;
import com.bachld.backend.config.ConnectedStudentRegistry;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.model.Classes;
import com.bachld.backend.model.ExamRoom;
import com.bachld.backend.model.PersonalComputer;
import com.bachld.backend.model.Student;
import com.bachld.backend.model.Teacher;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.ClassRepository;
import com.bachld.backend.repository.ExamRoomRepository;
import com.bachld.backend.repository.PersonalComputerRepository;
import com.bachld.backend.repository.StudentClassRepository;
import com.bachld.backend.repository.StudentExamRoomRepository;
import com.bachld.backend.repository.StudentRepository;
import com.bachld.backend.repository.TeacherRepository;
import com.bachld.backend.service.VncSessionService;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.auth.AuthFilter;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vnc")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VncController {

  VncSessionService vncSessionService;

  PersonalComputerRepository personalComputerRepository;

  ClassRepository classRepository;

  ExamRoomRepository examRoomRepository;

  StudentRepository studentRepository;

  StudentClassRepository studentClassRepository;

  StudentExamRoomRepository studentExamRoomRepository;

  TeacherRepository teacherRepository;

  ConnectedStudentRegistry connectedStudentRegistry;

  ConnectedExamStudentRegistry connectedExamStudentRegistry;

  Util util;

  @PostMapping("/v1/session/{classId}/{studentUserId}")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> createSession(
      @PathVariable int classId, @PathVariable int studentUserId) {

    Student student = validateClassAccess(classId, studentUserId);
    requireStudentOnline(
        connectedStudentRegistry.getConnectedStudents(classId).contains(student.getId()));
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(), Map.of("token", createRelayToken(studentUserId))));
  }

  @PostMapping("/v1/exam-room-session/{examRoomId}/{studentUserId}")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> createExamRoomSession(
      @PathVariable int examRoomId, @PathVariable int studentUserId) {

    Student student = validateExamRoomAccess(examRoomId, studentUserId);
    requireStudentOnline(
        connectedExamStudentRegistry.getConnectedStudents(examRoomId).contains(student.getId()));
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(), Map.of("token", createRelayToken(studentUserId))));
  }

  private String createRelayToken(Integer studentUserId) {
    PersonalComputer pc =
        personalComputerRepository
            .findByUserId(studentUserId)
            .orElseThrow(() -> new IllegalArgumentException("Student computer is not registered"));

    if (pc.getIpAddress() == null || pc.getIpAddress().isBlank()) {
      throw new IllegalStateException("Student computer has no IP address");
    }

    return vncSessionService.createSession(pc.getIpAddress());
  }

  private void requireStudentOnline(boolean online) {
    if (!online) {
      throw new IllegalStateException("Student desktop app is not connected");
    }
  }

  private Student validateClassAccess(Integer classId, Integer studentUserId) {
    Teacher teacher = getCurrentTeacher();
    Classes classes =
        classRepository
            .findById(classId)
            .orElseThrow(() -> new IllegalArgumentException("Class not found"));
    if (!Objects.equals(teacher.getId(), classes.getTeacherId())) {
      throw new IllegalArgumentException("Teacher cannot view this class");
    }

    Student student = getStudentByUserId(studentUserId);
    studentClassRepository
        .findByStudentIdAndClassId(student.getId(), classId)
        .orElseThrow(() -> new IllegalArgumentException("Student is not in this class"));
    return student;
  }

  private Student validateExamRoomAccess(Integer examRoomId, Integer studentUserId) {
    Teacher teacher = getCurrentTeacher();
    ExamRoom examRoom =
        examRoomRepository
            .findById(examRoomId)
            .orElseThrow(() -> new IllegalArgumentException("Exam room not found"));
    if (!Objects.equals(teacher.getId(), examRoom.getTeacher1Id())
        && !Objects.equals(teacher.getId(), examRoom.getTeacher2Id())) {
      throw new IllegalArgumentException("Teacher cannot view this exam room");
    }

    Student student = getStudentByUserId(studentUserId);
    studentExamRoomRepository
        .findByStudentIdAndExamRoomId(student.getId(), examRoomId)
        .orElseThrow(() -> new IllegalArgumentException("Student is not in this exam room"));
    return student;
  }

  private Teacher getCurrentTeacher() {
    User currentUser = util.getCurrentUser();
    if (currentUser == null) {
      throw new IllegalArgumentException("Invalid user");
    }
    return teacherRepository
        .findByUserId(currentUser.getId())
        .orElseThrow(() -> new IllegalArgumentException("Current teacher not found"));
  }

  private Student getStudentByUserId(Integer studentUserId) {
    return studentRepository
        .findByUserId(studentUserId)
        .orElseThrow(() -> new IllegalArgumentException("Student not found"));
  }
}
