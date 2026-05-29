package com.bachld.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Component
public class RsaKeyManager {

    @Value("${security.rsa-private-key}")
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
        log.warn("[RSA] security.rsa-private-key is not configured.");
        log.warn("[RSA] Generated a temporary RSA key pair. It will change when the server restarts.");
        log.warn("[RSA] To keep a stable key, add this property:");
        log.warn("[RSA] security.rsa-private-key={}", generatedPrivateKeyBase64);
        log.warn("=================================================================");
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public byte[] decryptBytes(String encryptedBase64) throws Exception {
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
        );
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);

        String[] chunks = encryptedBase64.split("\\|");
        java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
        for (String chunk : chunks) {
            byte[] encryptedBytes = Base64.getDecoder().decode(chunk.trim());
            result.writeBytes(cipher.doFinal(encryptedBytes));
        }
        return result.toByteArray();
    }

    public String decrypt(String encryptedBase64) throws Exception {
        return new String(decryptBytes(encryptedBase64), StandardCharsets.UTF_8);
    }

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
            throw new IllegalStateException("Cannot derive public key from the configured private key");
        }
    }
}
