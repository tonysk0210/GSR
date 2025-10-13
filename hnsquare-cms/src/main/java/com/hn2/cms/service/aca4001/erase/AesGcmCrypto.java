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

    // 配置為 Base64 字串放在環境變數或 application-*.properties
    private final SecretKey key;
    private static final int GCM_TAG_BITS = 128;   // 認證標籤長度 128 bits（標準建議值）
    private static final int GCM_IV_BYTES = 12;    // GCM 推薦 IV 長度：12 bytes（96 bits）

    public AesGcmCrypto(@Value("${erase.crypto.base64Key}") String base64Key) {
        // 1) 讀取並解 Base64；得到原始金鑰位元組
        byte[] raw = Base64.getDecoder().decode(base64Key);
        // 2) 驗證長度必須是 16/24/32 bytes（對應 AES-128/192/256）
        if (raw.length != 16 && raw.length != 24 && raw.length != 32) {
            throw new IllegalArgumentException("erase.crypto.base64Key must be 16/24/32 bytes after Base64 decode");
        }
        // 3) 建立對稱金鑰物件
        this.key = new SecretKeySpec(raw, "AES");
    }

    // 內部使用的小型回傳封裝：密文與 IV 的 Base64
    public static final class Encoded {
        public final String payloadBase64; // Base64(ciphertext)
        public final String ivBase64;      // Base64(12-byte IV)

        public Encoded(String payloadBase64, String ivBase64) {
            this.payloadBase64 = payloadBase64;
            this.ivBase64 = ivBase64;
        }
    }

    /**
     * 將 JSON（或任意字串）加密為 Base64 密文＋IV（AES-GCM），每次呼叫都會產生新的隨機 IV。
     */
    public Encoded encryptJson(String json) {
        // 1) 產生 12-byte 隨機 IV（GCM 建議長度）
        byte[] iv = new byte[GCM_IV_BYTES];
        new SecureRandom().nextBytes(iv);
        try {
            // 2) 取得 Cipher，設定為 AES/GCM/NoPadding
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            // 3) 以金鑰與 IV 初始化為加密模式，標籤長度 128 bits
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            // 4) 執行加密（UTF-8 編碼）
            byte[] ct = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
            // 5) 回傳 Base64(ciphertext) 與 Base64(IV)
            return new Encoded(Base64.getEncoder().encodeToString(ct),
                    Base64.getEncoder().encodeToString(iv));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM encrypt failed", e);
        }
    }

    /**
     * 以 Base64 密文＋IV 解回原始 JSON 字串（同金鑰、同 GCM 參數）。
     */
    public String decryptToJson(String payloadBase64, String ivBase64) {
        try {
            // 1) 解 Base64 取得 IV 與密文
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] ct = Base64.getDecoder().decode(payloadBase64);
            // 2) 取得 Cipher 並初始化為解密模式（同樣的標籤長度與 IV）
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            // 3) 執行解密與驗證；若密文/IV/標籤被竄改會在此丟出 AEADBadTagException
            byte[] pt = cipher.doFinal(ct);
            // 4) 回傳 UTF-8 字串
            return new String(pt, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM decrypt failed", e);
        }
    }

    /**
     * 對任意字串計算 SHA-256 並以 64 位十六進位小寫字串回傳。
     */
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