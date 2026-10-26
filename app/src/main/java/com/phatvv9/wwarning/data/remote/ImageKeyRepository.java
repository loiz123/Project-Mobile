package com.phatvv9.wwarning.data.remote;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.phatvv9.wwarning.util.ImageCrypto;

import java.util.Base64;

/**
 * Lấy khóa AES từ Firestore: secrets/image_key, field "value" (Base64 của 32 byte).
 * Rules chỉ cho người đã đăng nhập đọc, không ai ghi được từ app. Khóa không nằm trong mã nguồn.
 */
public final class ImageKeyRepository {

    public interface KeyCallback {
        void onKey(@NonNull byte[] key);
        void onError(@NonNull String message);
    }

    private static volatile ImageKeyRepository INSTANCE;
    private volatile byte[] cachedKey;

    public static ImageKeyRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (ImageKeyRepository.class) {
                if (INSTANCE == null) INSTANCE = new ImageKeyRepository();
            }
        }
        return INSTANCE;
    }

    private ImageKeyRepository() {}

    public void getKey(@NonNull final KeyCallback callback) {
        byte[] key = cachedKey;
        if (key != null) {
            callback.onKey(key);
            return;
        }
        FirebaseFirestore.getInstance().collection("secrets").document("image_key").get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot snap) {
                        String value = snap.exists() ? snap.getString("value") : null;
                        if (value == null) {
                            callback.onError("Chưa cấu hình khóa mã hóa ảnh (secrets/image_key)");
                            return;
                        }
                        try {
                            byte[] decoded = Base64.getDecoder().decode(value.trim());
                            if (decoded.length != ImageCrypto.KEY_BYTES) {
                                callback.onError("Khóa mã hóa ảnh phải dài 32 byte");
                                return;
                            }
                            cachedKey = decoded;
                            callback.onKey(decoded);
                        } catch (IllegalArgumentException e) {
                            callback.onError("Khóa mã hóa ảnh không phải Base64 hợp lệ");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError("Không lấy được khóa mã hóa: " + e.getMessage());
                    }
                });
    }
}
