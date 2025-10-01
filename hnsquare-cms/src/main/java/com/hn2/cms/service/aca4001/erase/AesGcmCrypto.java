package com.hn2.cms.service.aca4001.erase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmCrypto {

    // 建議 32 bytes（AES-256），以 Base64 放在環境變數／application-*.properties
    private final SecretKey key;
    private static final int GCM_TAG_BITS = 128;   // 16-byte tag
    private static final int GCM_IV_BYTES = 12;    // 12-byte IV (96 bits)

    public AesGcmCrypto(@Value("${erase.crypto.base64Key}") String base64Key) {
        byte[] raw = Base64.getDecoder().decode(base64Key);
        if (raw.length != 16 && raw.length != 24 && raw.length != 32) {
            throw new IllegalArgumentException("erase.crypto.base64Key must be 16/24/32 bytes after Base64 decode");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public static final class Encoded {
        public final String payloadBase64; // Base64(ciphertext)
        public final String ivBase64;      // Base64(12-byte IV)
        public Encoded(String payloadBase64, String ivBase64) {
            this.payloadBase64 = payloadBase64;
            this.ivBase64 = ivBase64;
        }
    }

    /** 將 JSON（或任意字串）加密為 Base64 密文＋IV */
    public Encoded encryptJson(String json) {
        byte[] iv = new byte[GCM_IV_BYTES];
        new SecureRandom().nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
            return new Encoded(Base64.getEncoder().encodeToString(ct),
                    Base64.getEncoder().encodeToString(iv));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM encrypt failed", e);
        }
    }

    /** 以 Base64 密文＋IV 解回原始 JSON 字串 */
    public String decryptToJson(String payloadBase64, String ivBase64) {
        try {
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] ct = Base64.getDecoder().decode(payloadBase64);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM decrypt failed", e);
        }
    }

    /** 給你用的 SHA-256（如果你想放在 Service 裡也行） */
    public static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}