package com.bachld.service;

import com.bachld.model.request.ClipboardEventPayload;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class ClipboardEncryptionService {

    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    public ClipboardEventPayload encrypt(String applicationName, int action, String clipboardText, String publicKeyBase64) throws Exception {
        PublicKey publicKey = parsePublicKey(publicKeyBase64);

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey aesKey = keyGenerator.generateKey();

        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);

        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ciphertext = aesCipher.doFinal(clipboardText.getBytes(StandardCharsets.UTF_8));

        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
        );
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);
        byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

        return new ClipboardEventPayload(
                applicationName,
                action,
                Base64.getEncoder().encodeToString(ciphertext),
                Base64.getEncoder().encodeToString(encryptedAesKey),
                Base64.getEncoder().encodeToString(iv)
        );
    }

    private PublicKey parsePublicKey(String publicKeyBase64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64.trim());
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
    }
}
