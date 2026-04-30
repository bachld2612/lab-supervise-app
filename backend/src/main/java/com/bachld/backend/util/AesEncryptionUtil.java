package com.bachld.backend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesEncryptionUtil {

    @Value("${veyon.secret-key}")
    private String secretKeyStr;

    private SecretKeySpec buildKey() throws Exception {
        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                .digest(secretKeyStr.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts plaintext with AES-256-CBC.
     * Stored format: Base64(16-byte-IV + encrypted-data)
     */
    public String encrypt(String data) throws Exception {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[16 + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, 16);
        System.arraycopy(encrypted, 0, combined, 16, encrypted.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypts AES-256-CBC encrypted data from {@link #encrypt}.
     */
    public String decrypt(String encryptedData) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedData);

        byte[] iv = new byte[16];
        byte[] encrypted = new byte[combined.length - 16];
        System.arraycopy(combined, 0, iv, 0, 16);
        System.arraycopy(combined, 16, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, buildKey(), new IvParameterSpec(iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }
}