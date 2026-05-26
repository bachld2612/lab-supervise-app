package com.bachld.backend.service;

import com.bachld.backend.dto.request.PersonalComputerUpdateRequest;
import com.bachld.backend.dto.response.PersonalComputerResponse;
import com.bachld.backend.dto.response.StudentPcInfoResponse;
import com.bachld.backend.model.PersonalComputer;
import com.bachld.backend.model.Student;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.PersonalComputerRepository;
import com.bachld.backend.repository.StudentClassRepository;
import com.bachld.backend.repository.StudentExamRoomRepository;
import com.bachld.backend.repository.StudentRepository;
import com.bachld.backend.repository.UserRepository;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.VncPasswordCipher;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonalComputerService {

    PersonalComputerRepository personalComputerRepository;
    StudentClassRepository studentClassRepository;
    StudentExamRoomRepository studentExamRoomRepository;
    StudentRepository studentRepository;
    UserRepository userRepository;
    VncPasswordCipher vncPasswordCipher;
    Util util;

    public void update(PersonalComputerUpdateRequest request) {
        User currentUser = util.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("Người dùng không hợp lệ");
        }

        Integer userId = currentUser.getId();

        PersonalComputer pc = personalComputerRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PersonalComputer newPc = new PersonalComputer();
                    newPc.setUserId(userId);
                    newPc.setStatus(Status.ACTIVE.getValue());
                    return newPc;
                });

        pc.setIpAddress(request.getIpAddress());

        if (request.getVncPassword() != null && !request.getVncPassword().isBlank()) {
            pc.setVncPasswordEncrypted(request.getVncPassword());
            log.info("VNC password updated for user {} via /v1/update", userId);
        }

        personalComputerRepository.save(pc);
    }

    public PersonalComputerResponse getByUserId() {
        User currentUser = util.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("Người dùng không hợp lệ");
        }

        return personalComputerRepository.findByUserId(currentUser.getId())
                .map(pc -> new PersonalComputerResponse(pc.getIpAddress(), pc.getUserId()))
                .orElse(null);
    }

    public List<StudentPcInfoResponse> getStudentsByClassId(Integer classId) {
        return studentClassRepository.findByClassId(classId).stream()
                .map(sc -> buildStudentPcInfo(sc.getStudentId()))
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    public List<StudentPcInfoResponse> getStudentsByExamRoomId(Integer examRoomId) {
        return studentExamRoomRepository.findByExamRoomId(examRoomId).stream()
                .map(ser -> buildStudentPcInfo(ser.getStudentId()))
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    public void registerVncPassword(String vncPasswordPlaintext) {
        User currentUser = util.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("Người dùng không hợp lệ");
        }

        Integer userId = currentUser.getId();

        PersonalComputer pc = personalComputerRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PersonalComputer newPc = new PersonalComputer();
                    newPc.setUserId(userId);
                    newPc.setStatus(Status.ACTIVE.getValue());
                    return newPc;
                });

        pc.setVncPasswordEncrypted(vncPasswordPlaintext);
        personalComputerRepository.save(pc);
        log.info("VNC password registered for user {}", userId);
    }

    public String resolveVncPasswordByUserId(Integer userId) {
        return personalComputerRepository.findByUserId(userId)
                .map(PersonalComputer::getVncPasswordEncrypted)
                .filter(pwd -> pwd != null && !pwd.isEmpty())
                .orElse(null);
    }

    public void updateStudentPcByUserId(Integer targetUserId, String ipAddress) {
        PersonalComputer pc = personalComputerRepository.findByUserId(targetUserId)
                .orElseGet(() -> {
                    PersonalComputer newPc = new PersonalComputer();
                    newPc.setUserId(targetUserId);
                    newPc.setStatus(Status.ACTIVE.getValue());
                    return newPc;
                });
        pc.setIpAddress(ipAddress);
        personalComputerRepository.save(pc);
    }

    private StudentPcInfoResponse buildStudentPcInfo(Integer studentId) {
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) return null;

        User user = userRepository.findById(student.getUserId()).orElse(null);
        String fullName = user != null ? user.getFullName() : "";

        String ipAddress = personalComputerRepository.findByUserId(student.getUserId())
                .map(PersonalComputer::getIpAddress)
                .orElse(null);

        return new StudentPcInfoResponse(studentId, student.getUserId(), fullName, student.getCode(), ipAddress);
    }
}
