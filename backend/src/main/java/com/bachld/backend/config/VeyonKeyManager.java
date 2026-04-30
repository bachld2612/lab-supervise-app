package com.bachld.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Component
public class VeyonKeyManager {

    @Value("${veyon.rsa-private-key:}")
    private String privateKeyBase64;

    private PrivateKey privateKey;
    
    private PublicKey publicKey;

    @PostConstruct
    public void init() throws Exception {
        if (privateKeyBase64 != null && !privateKeyBase64.isBlank()) {
            loadFromProperties();
        } else {
            generateAndLogNewKeyPair();
        }
    }

    private void loadFromProperties() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64.trim());
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.privateKey = keyFactory.generatePrivate(keySpec);

        // Derive public key from private key
        RSAPrivateKeyInfo info = new RSAPrivateKeyInfo(this.privateKey);
        this.publicKey = info.derivePublicKey();
    }

    private void generateAndLogNewKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();

        String generatedPrivateKeyBase64 = Base64.getEncoder().encodeToString(this.privateKey.getEncoded());
        log.warn("=================================================================");
        log.warn("[VEYON] veyon.rsa-private-key chưa được cấu hình!");
        log.warn("[VEYON] Đã tạo cặp khóa RSA tạm thời. Khóa sẽ thay đổi khi restart server.");
        log.warn("[VEYON] Để cố định khóa, hãy thêm dòng sau vào application.properties:");
        log.warn("[VEYON] veyon.rsa-private-key={}", generatedPrivateKeyBase64);
        log.warn("=================================================================");
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Decrypts RSA-OAEP-SHA256 encrypted data sent by FE.
     * FE must split the PEM string into 190-byte chunks, RSA-encrypt each chunk,
     * Base64-encode each encrypted chunk, and join them with the "|" separator.
     */
    public String decrypt(String encryptedBase64) throws Exception {
        // Dùng OAEPParameterSpec tường minh: SHA-256 cho cả OAEP hash lẫn MGF1
        // để khớp với Web Crypto API bên FE (mặc định Java MGF1 dùng SHA-1 gây lỗi)
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
        );
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);

        String[] chunks = encryptedBase64.split("\\|");
        StringBuilder result = new StringBuilder();
        for (String chunk : chunks) {
            byte[] encryptedBytes = Base64.getDecoder().decode(chunk.trim());
            result.append(new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8));
        }
        return result.toString();
    }

    // Helper to derive RSA public key from private key
    private static class RSAPrivateKeyInfo {
        private final PrivateKey privateKey;

        RSAPrivateKeyInfo(PrivateKey privateKey) {
            this.privateKey = privateKey;
        }

        PublicKey derivePublicKey() throws Exception {
            if (privateKey instanceof java.security.interfaces.RSAPrivateCrtKey crtKey) {
                java.security.spec.RSAPublicKeySpec publicKeySpec =
                        new java.security.spec.RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent());
                return KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
            }
            throw new IllegalStateException("Không thể derive public key từ private key đã cho");
        }
    }
}