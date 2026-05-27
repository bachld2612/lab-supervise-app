package com.bachld.backend.service;

import com.bachld.backend.config.VeyonKeyManager;
import com.bachld.backend.dto.request.ImportVeyonKeyRequest;
import com.bachld.backend.dto.request.LockScreenExamRoomRequest;
import com.bachld.backend.dto.request.LockScreenRequest;
import com.bachld.backend.dto.request.OpenWebsiteRequest;
import com.bachld.backend.dto.request.SendMessageRequest;
import com.bachld.backend.dto.response.ScreenshotCaptureResponse;
import com.bachld.backend.model.*;
import com.bachld.backend.repository.*;
import com.bachld.backend.util.AesEncryptionUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VeyonService {

    VeyonKeyManager veyonKeyManager;

    AesEncryptionUtil aesEncryptionUtil;

    VeyonClientService veyonClientService;

    ScreenshotCaptureService screenshotCaptureService;

    ClassRepository classRepository;

    TeacherRepository teacherRepository;

    PersonalComputerRepository personalComputerRepository;

    StudentClassRepository studentClassRepository;

    StudentRepository studentRepository;

    ExamRoomRepository examRoomRepository;

    StudentExamRoomRepository studentExamRoomRepository;

    public String getPublicKey() {
        return veyonKeyManager.getPublicKeyBase64();
    }

    @Transactional
    public void importKey(ImportVeyonKeyRequest request) {
        Classes classes = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học có id: " + request.getClassId()));

        String decryptedKeyContent;
        try {
            decryptedKeyContent = veyonKeyManager.decrypt(request.getEncryptedKeyData());
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Không thể giải mã dữ liệu khóa: " + e.getMessage());
        }

        String encryptedForStorage;
        try {
            encryptedForStorage = aesEncryptionUtil.encrypt(decryptedKeyContent);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi mã hóa khóa để lưu trữ: " + e.getMessage());
        }

        classes.setVeyonKeyName(request.getKeyName());
        classes.setVeyonKey(encryptedForStorage);
        classRepository.save(classes);
    }

    public void lockScreen(LockScreenRequest request) {
        String[] credentials = getVeyonCredentials(request.getClassId());
        String studentIp = getStudentIp(request.getStudentUserId());
        String connectionUid = veyonClientService.getOrReuseConnectionUid(credentials[0], credentials[1], credentials[2], studentIp);
        veyonClientService.lockScreen(connectionUid, request.getActive(), credentials[2]);
    }

    public void openWebsiteForStudent(Integer classId, Integer studentId, OpenWebsiteRequest request) {
        String[] credentials = getVeyonCredentials(classId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên có id: " + studentId));
        PersonalComputer pc = personalComputerRepository.findByUserId(student.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Sinh viên chưa đăng ký máy tính cá nhân"));
        String connectionUid = veyonClientService.getOrReuseConnectionUid(credentials[0], credentials[1], credentials[2], pc.getIpAddress());
        veyonClientService.openWebsite(connectionUid, request.getWebsiteUrl(), credentials[2]);
    }

    public void openWebsiteForClass(Integer classId, OpenWebsiteRequest request) {
        String[] credentials = getVeyonCredentials(classId);
        List<StudentClass> studentClasses = studentClassRepository.findByClassId(classId);

        for (StudentClass sc : studentClasses) {
            try {
                Student student = studentRepository.findById(sc.getStudentId()).orElse(null);
                if (student == null) continue;

                PersonalComputer pc = personalComputerRepository.findByUserId(student.getUserId()).orElse(null);
                if (pc == null) continue;

                String connectionUid = veyonClientService.getOrReuseConnectionUid(credentials[0], credentials[1], credentials[2], pc.getIpAddress());
                veyonClientService.openWebsite(connectionUid, request.getWebsiteUrl(), credentials[2]);
            } catch (Exception ignored) {
            }
        }
    }

    public void sendMessageForStudent(Integer classId, Integer studentId, SendMessageRequest request) {
        String[] credentials = getVeyonCredentials(classId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên có id: " + studentId));
        PersonalComputer pc = personalComputerRepository.findByUserId(student.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Sinh viên chưa đăng ký máy tính cá nhân"));
        String connectionUid = veyonClientService.getOrReuseConnectionUid(credentials[0], credentials[1], credentials[2], pc.getIpAddress());
        veyonClientService.sendMessage(connectionUid, request.getText(), credentials[2]);
    }

    public void sendMessageForClass(Integer classId, SendMessageRequest request) {
        String[] credentials = getVeyonCredentials(classId);
        List<StudentClass> studentClasses = studentClassRepository.findByClassId(classId);

        for (StudentClass sc : studentClasses) {
            try {
                Student student = studentRepository.findById(sc.getStudentId()).orElse(null);
                if (student == null) continue;

                PersonalComputer pc = personalComputerRepository.findByUserId(student.getUserId()).orElse(null);
                if (pc == null) continue;

                String connectionUid = veyonClientService.getOrReuseConnectionUid(credentials[0], credentials[1], credentials[2], pc.getIpAddress());
                veyonClientService.sendMessage(connectionUid, request.getText(), credentials[2]);
            } catch (Exception ignored) {
            }
        }
    }

    public ScreenshotCaptureResponse getScreenshot(Integer classId, Integer studentUserId) {
        return screenshotCaptureService.requestClassScreenshot(classId, studentUserId);
    }

    // ===== EXAM ROOM VEYON METHODS =====

    @Transactional
    public void importKeyForExamRoom(Integer examRoomId, String keyName, String encryptedKeyData) {
        ExamRoom examRoom = examRoomRepository.findById(examRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng thi có id: " + examRoomId));

        String decryptedKeyContent;
        try {
            decryptedKeyContent = veyonKeyManager.decrypt(encryptedKeyData);
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể giải mã dữ liệu khóa: " + e.getMessage());
        }

        String encryptedForStorage;
        try {
            encryptedForStorage = aesEncryptionUtil.encrypt(decryptedKeyContent);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi mã hóa khóa để lưu trữ: " + e.getMessage());
        }

        examRoom.setVeyonKeyName(keyName);
        examRoom.setVeyonKey(encryptedForStorage);
        examRoomRepository.save(examRoom);
    }

    public void lockScreenForExamRoom(LockScreenExamRoomRequest request) {
        String[] credentials = getVeyonCredentialsForExamRoom(request.getExamRoomId());
        String studentIp = getStudentIp(request.getStudentUserId());
        String connectionUid = veyonClientService.getOrReuseConnectionUid(credentials[0], credentials[1], credentials[2], studentIp);
        veyonClientService.lockScreen(connectionUid, request.getActive(), credentials[2]);
    }

    public ScreenshotCaptureResponse getScreenshotForExamRoom(Integer examRoomId, Integer studentUserId) {
        return screenshotCaptureService.requestExamRoomScreenshot(examRoomId, studentUserId);
    }

    public void openWebsiteForExamRoom(Integer examRoomId, OpenWebsiteRequest request) {
        String[] credentials = getVeyonCredentialsForExamRoom(examRoomId);
        List<StudentExamRoom> enrolled = studentExamRoomRepository.findByExamRoomId(examRoomId);
        for (StudentExamRoom ser : enrolled) {
            try {
                Student student = studentRepository.findById(ser.getStudentId()).orElse(null);
                if (student == null) continue;
                PersonalComputer pc = personalComputerRepository.findByUserId(student.getUserId()).orElse(null);
                if (pc == null) continue;
                String connectionUid = veyonClientService.getOrReuseConnectionUid(credentials[0], credentials[1], credentials[2], pc.getIpAddress());
                veyonClientService.openWebsite(connectionUid, request.getWebsiteUrl(), credentials[2]);
            } catch (Exception ignored) {
            }
        }
    }

    public void openWebsiteForExamRoomStudent(Integer examRoomId, Integer studentId, OpenWebsiteRequest request) {
        String[] credentials = getVeyonCredentialsForExamRoom(examRoomId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên có id: " + studentId));
        PersonalComputer pc = personalComputerRepository.findByUserId(student.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Sinh viên chưa đăng ký máy tính cá nhân"));
        String connectionUid = veyonClientService.getOrReuseConnectionUid(credentials[0], credentials[1], credentials[2], pc.getIpAddress());
        veyonClientService.openWebsite(connectionUid, request.getWebsiteUrl(), credentials[2]);
    }

    public void sendMessageForExamRoom(Integer examRoomId, SendMessageRequest request) {
        String[] credentials = getVeyonCredentialsForExamRoom(examRoomId);
        List<StudentExamRoom> enrolled = studentExamRoomRepository.findByExamRoomId(examRoomId);
        for (StudentExamRoom ser : enrolled) {
            try {
                Student student = studentRepository.findById(ser.getStudentId()).orElse(null);
                if (student == null) continue;
                PersonalComputer pc = personalComputerRepository.findByUserId(student.getUserId()).orElse(null);
                if (pc == null) continue;
                String connectionUid = veyonClientService.getOrReuseConnectionUid(credentials[0], credentials[1], credentials[2], pc.getIpAddress());
                veyonClientService.sendMessage(connectionUid, request.getText(), credentials[2]);
            } catch (Exception ignored) {
            }
        }
    }

    public void sendMessageForExamRoomStudent(Integer examRoomId, Integer studentId, SendMessageRequest request) {
        String[] credentials = getVeyonCredentialsForExamRoom(examRoomId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên có id: " + studentId));
        PersonalComputer pc = personalComputerRepository.findByUserId(student.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Sinh viên chưa đăng ký máy tính cá nhân"));
        String connectionUid = veyonClientService.getOrReuseConnectionUid(credentials[0], credentials[1], credentials[2], pc.getIpAddress());
        veyonClientService.sendMessage(connectionUid, request.getText(), credentials[2]);
    }

    private String[] getVeyonCredentialsForExamRoom(Integer examRoomId) {
        ExamRoom examRoom = examRoomRepository.findById(examRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng thi có id: " + examRoomId));

        if (examRoom.getVeyonKey() == null || examRoom.getVeyonKeyName() == null) {
            throw new IllegalArgumentException("Phòng thi chưa được cấu hình khóa Veyon");
        }

        String decryptedKey;
        try {
            decryptedKey = aesEncryptionUtil.decrypt(examRoom.getVeyonKey());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi giải mã khóa Veyon: " + e.getMessage());
        }

        // Dùng teacher1 làm host cho Veyon
        Teacher teacher = teacherRepository.findById(examRoom.getTeacher1Id())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên coi thi"));

        PersonalComputer teacherPc = personalComputerRepository.findByUserId(teacher.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Giảng viên chưa đăng ký máy tính cá nhân"));

        return new String[]{examRoom.getVeyonKeyName(), decryptedKey, teacherPc.getIpAddress()};
    }

    // Returns [keyName, decryptedKeyContent, teacherIp]
    private String[] getVeyonCredentials(Integer classId) {
        Classes classes = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học có id: " + classId));

        if (classes.getVeyonKey() == null || classes.getVeyonKeyName() == null) {
            throw new IllegalArgumentException("Lớp học chưa được cấu hình khóa Veyon");
        }

        String decryptedKey;
        try {
            decryptedKey = aesEncryptionUtil.decrypt(classes.getVeyonKey());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi giải mã khóa Veyon: " + e.getMessage());
        }

        Teacher teacher = teacherRepository.findById(classes.getTeacherId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giáo viên của lớp: " + classId));

        PersonalComputer teacherPc = personalComputerRepository.findByUserId(teacher.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Giáo viên chưa đăng ký máy tính cá nhân"));

        return new String[]{classes.getVeyonKeyName(), decryptedKey, teacherPc.getIpAddress()};
    }

    private String getStudentIp(Integer studentUserId) {
        return personalComputerRepository.findByUserId(studentUserId)
                .map(PersonalComputer::getIpAddress)
                .orElseThrow(() -> new IllegalArgumentException("Sinh viên chưa nhập IP"));
    }
}
