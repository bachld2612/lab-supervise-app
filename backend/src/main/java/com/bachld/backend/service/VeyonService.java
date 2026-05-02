package com.bachld.backend.service;

import com.bachld.backend.config.VeyonKeyManager;
import com.bachld.backend.dto.request.ImportVeyonKeyRequest;
import com.bachld.backend.dto.request.LockScreenRequest;
import com.bachld.backend.model.Classes;
import com.bachld.backend.model.PersonalComputer;
import com.bachld.backend.model.Teacher;
import com.bachld.backend.repository.ClassRepository;
import com.bachld.backend.repository.PersonalComputerRepository;
import com.bachld.backend.repository.TeacherRepository;
import com.bachld.backend.util.AesEncryptionUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VeyonService {

    VeyonKeyManager veyonKeyManager;

    AesEncryptionUtil aesEncryptionUtil;

    VeyonClientService veyonClientService;

    ClassRepository classRepository;

    TeacherRepository teacherRepository;

    PersonalComputerRepository personalComputerRepository;

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
        String connectionUid = veyonClientService.getConnectionUid(credentials[0], credentials[1], credentials[2], studentIp);
        veyonClientService.lockScreen(connectionUid, request.getActive(), credentials[2]);
    }

    public String getScreenshot(Integer classId, Integer studentUserId) {
        String[] credentials = getVeyonCredentials(classId);
        String studentIp = getStudentIp(studentUserId);
        String connectionUid = veyonClientService.getConnectionUid(credentials[0], credentials[1], credentials[2], studentIp);
        byte[] imageBytes = veyonClientService.getScreenshot(connectionUid, credentials[2]);
        return Base64.getEncoder().encodeToString(imageBytes);
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy máy tính của sinh viên có userId: " + studentUserId));
    }
}