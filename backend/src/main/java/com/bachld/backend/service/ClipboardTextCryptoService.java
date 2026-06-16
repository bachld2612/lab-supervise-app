package com.bachld.backend.service;

import com.bachld.backend.config.RsaKeyManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClipboardTextCryptoService {

    static final int GCM_TAG_BITS = 128;

    RsaKeyManager rsaKeyManager;

    public String decrypt(String ciphertextBase64, String encryptedKeyBase64, String ivBase64) {
        if (isBlank(ciphertextBase64) || isBlank(encryptedKeyBase64) || isBlank(ivBase64)) {
            return null;
        }

        try {
            byte[] aesKey = rsaKeyManager.decryptBytes(encryptedKeyBase64);
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] ciphertext = Base64.getDecoder().decode(ciphertextBase64);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot decrypt clipboard text: " + e.getMessage(), e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
