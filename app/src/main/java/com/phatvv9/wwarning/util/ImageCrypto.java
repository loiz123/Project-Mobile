package com.phatvv9.wwarning.util;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Mã hóa ảnh bằng AES-256-GCM. Mỗi ảnh có IV ngẫu nhiên 12 byte, ghép vào đầu dữ liệu.
 * Kết quả dạng chuỗi Base64 để lưu trong field text của Firestore.
 * Lớp thuần Java (không phụ thuộc Android) nên test được bằng JUnit thường.
 */
public final class ImageCrypto {
    public static final int KEY_BYTES = 32;
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RNG = new SecureRandom();

    private ImageCrypto() {}

    public static String encryptToBase64(byte[] plain, byte[] key) throws GeneralSecurityException {
        checkKey(key);
        byte[] iv = new byte[IV_LEN];
        RNG.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(TAG_BITS, iv));
        byte[] enc = cipher.doFinal(plain);
        byte[] out = new byte[IV_LEN + enc.length];
        System.arraycopy(iv, 0, out, 0, IV_LEN);
        System.arraycopy(enc, 0, out, IV_LEN, enc.length);
        return Base64.getEncoder().encodeToString(out);
    }

    public static byte[] decryptFromBase64(String base64, byte[] key) throws GeneralSecurityException {
        checkKey(key);
        byte[] data = Base64.getDecoder().decode(base64);
        if (data.length <= IV_LEN + TAG_BITS / 8) {
            throw new GeneralSecurityException("Dữ liệu ảnh không hợp lệ");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(TAG_BITS, data, 0, IV_LEN));
        return cipher.doFinal(data, IV_LEN, data.length - IV_LEN);
    }

    private static void checkKey(byte[] key) {
        if (key == null || key.length != KEY_BYTES) {
            throw new IllegalArgumentException("Khóa phải dài đúng " + KEY_BYTES + " byte");
        }
    }
}
