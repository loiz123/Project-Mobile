package com.phatvv9.wwarning.data.remote;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.phatvv9.wwarning.util.ImageCodec;
import com.phatvv9.wwarning.util.ImageCrypto;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ảnh của báo cáo nằm ở collection riêng: report_images/{reportId} gồm
 *   userId (chủ ảnh), imageData = Base64( IV + AES-GCM( JPEG đã nén ) ), createdAt.
 * Tách khỏi "reports" để listener Geohash trên bản đồ KHÔNG phải tải ảnh;
 * ảnh chỉ được tải khi người dùng mở chi tiết một báo cáo.
 * Mọi callback đều chạy trên main thread.
 */
public class ReportImageRepository {

    public interface Callback<T> {
        void onResult(@Nullable T value);
        void onError(@NonNull String message);
    }

    public static final String COLLECTION_IMAGES = "report_images";
    public static final String FIELD_IMAGE = "imageData";

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final ImageKeyRepository keys = ImageKeyRepository.getInstance();

    /** Nén, mã hóa rồi ghi ảnh. Chỉ chủ báo cáo ghi được, mỗi báo cáo một ảnh (xem firestore.rules). */
    public void uploadImage(@NonNull Context context, @NonNull final String reportId,
                            @NonNull final Uri uri, @NonNull final Callback<Void> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Bạn cần đăng nhập để thêm ảnh");
            return;
        }
        final String uid = user.getUid();
        final Context app = context.getApplicationContext();

        keys.getKey(new ImageKeyRepository.KeyCallback() {
            @Override
            public void onKey(@NonNull final byte[] key) {
                IO.execute(new Runnable() {
                    @Override
                    public void run() {
                        final String payload;
                        try {
                            byte[] jpeg = ImageCodec.compressFromUri(app, uri);
                            payload = ImageCrypto.encryptToBase64(jpeg, key);
                        } catch (Exception e) {
                            postError(callback, "Không xử lý được ảnh: " + e.getMessage());
                            return;
                        }
                        MAIN.post(new Runnable() {
                            @Override
                            public void run() {
                                Map<String, Object> doc = new HashMap<>();
                                doc.put("userId", uid);
                                doc.put(FIELD_IMAGE, payload);
                                doc.put("createdAt", FieldValue.serverTimestamp());
                                db.collection(COLLECTION_IMAGES).document(reportId).set(doc)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                callback.onResult(null);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                callback.onError("Tải ảnh lên thất bại: " + e.getMessage());
                                            }
                                        });
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(@NonNull String message) {
                callback.onError(message);
            }
        });
    }

    /** Tải và giải mã ảnh của một báo cáo. onResult(null) nếu báo cáo chưa có ảnh. */
    public void loadImage(@NonNull String reportId, @NonNull final Callback<Bitmap> callback) {
        db.collection(COLLECTION_IMAGES).document(reportId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot snap) {
                        final String data = snap.exists() ? snap.getString(FIELD_IMAGE) : null;
                        if (data == null || data.isEmpty()) {
                            callback.onResult(null);
                            return;
                        }
                        keys.getKey(new ImageKeyRepository.KeyCallback() {
                            @Override
                            public void onKey(@NonNull final byte[] key) {
                                IO.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            byte[] jpeg = ImageCrypto.decryptFromBase64(data, key);
                                            final Bitmap bitmap = ImageCodec.decode(jpeg);
                                            MAIN.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    callback.onResult(bitmap);
                                                }
                                            });
                                        } catch (Exception e) {
                                            postError(callback, "Không giải mã được ảnh");
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onError(@NonNull String message) {
                                callback.onError(message);
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError("Không tải được ảnh: " + e.getMessage());
                    }
                });
    }

    private static void postError(final Callback<?> callback, final String message) {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(message);
            }
        });
    }
}
