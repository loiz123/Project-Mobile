package com.phatvv9.wwarning;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import com.phatvv9.wwarning.util.ImageCrypto;

import org.junit.Test;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public class ImageCryptoTest {

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        new SecureRandom().nextBytes(b);
        return b;
    }

    @Test
    public void encryptThenDecrypt_returnsOriginal() throws Exception {
        byte[] key = randomBytes(ImageCrypto.KEY_BYTES);
        byte[] image = randomBytes(50_000);
        String enc = ImageCrypto.encryptToBase64(image, key);
        assertArrayEquals(image, ImageCrypto.decryptFromBase64(enc, key));
    }

    @Test
    public void sameImageEncryptedTwice_givesDifferentText() throws Exception {
        byte[] key = randomBytes(ImageCrypto.KEY_BYTES);
        byte[] image = randomBytes(1_000);
        assertNotEquals(ImageCrypto.encryptToBase64(image, key),
                ImageCrypto.encryptToBase64(image, key));   // IV ngẫu nhiên
    }

    @Test
    public void wrongKey_isRejected() throws Exception {
        String enc = ImageCrypto.encryptToBase64(randomBytes(1_000), randomBytes(32));
        try {
            ImageCrypto.decryptFromBase64(enc, randomBytes(32));
            fail("Phải ném lỗi khi sai khóa");
        } catch (GeneralSecurityException expected) {
            // đúng như mong đợi
        }
    }

    @Test
    public void tamperedData_isRejected() throws Exception {
        byte[] key = randomBytes(32);
        char[] chars = ImageCrypto.encryptToBase64(randomBytes(1_000), key).toCharArray();
        int i = chars.length / 2;
        chars[i] = chars[i] == 'A' ? 'B' : 'A';   // sửa 1 ký tự giữa chuỗi
        try {
            ImageCrypto.decryptFromBase64(new String(chars), key);
            fail("GCM phải phát hiện dữ liệu bị sửa");
        } catch (GeneralSecurityException expected) {
            // đúng như mong đợi
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void shortKey_isRejected() throws Exception {
        ImageCrypto.encryptToBase64(new byte[10], new byte[16]);
    }
}
