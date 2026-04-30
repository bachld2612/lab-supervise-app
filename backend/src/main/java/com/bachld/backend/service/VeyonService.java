package com.bachld.backend.service;

import com.bachld.backend.config.VeyonKeyManager;
import com.bachld.backend.dto.request.ImportVeyonKeyRequest;
import com.bachld.backend.dto.request.LockScreenRequest;
import com.bachld.backend.model.Classes;
import com.bachld.backend.repository.ClassRepository;
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
        String connectionUid = veyonClientService.getConnectionUid(credentials[0], credentials[1]);
        veyonClientService.lockScreen(connectionUid, request.getActive());
    }

    public String getScreenshot(Integer classId) {
        String[] credentials = getVeyonCredentials(classId);
        String connectionUid = veyonClientService.getConnectionUid(credentials[0], credentials[1]);
        byte[] imageBytes = veyonClientService.getScreenshot(connectionUid);
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    // Returns [keyName, decryptedKeyContent]
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

        return new String[]{classes.getVeyonKeyName(), decryptedKey};
    }
}