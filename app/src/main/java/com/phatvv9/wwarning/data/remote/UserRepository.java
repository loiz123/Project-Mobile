package com.phatvv9.wwarning.data.remote;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.phatvv9.wwarning.model.UserProfile;
import com.phatvv9.wwarning.util.FirestoreNames;

import java.util.HashMap;
import java.util.Map;

/**
 * Quản lý users/{uid} và user_devices/{uid}_{deviceId}.
 * Dùng chung một thể hiện (getInstance) để mọi màn hình cùng thấy một hồ sơ hiện tại.
 */
public class UserRepository {

    private static final String TAG = "WWarning";

    public interface UserCallback {
        void onSuccess(@NonNull UserProfile profile);

        void onError(@NonNull Exception e);
    }

    public interface DoneCallback {
        void onDone();

        void onError(@NonNull Exception e);
    }

    public interface CountCallback {
        void onCount(long count);

        void onError(@NonNull Exception e);
    }

    private static UserRepository instance;

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<UserProfile> currentProfile = new MutableLiveData<>();
    private ListenerRegistration profileListener;

    private UserRepository() {}

    /** Hồ sơ của người dùng đang đăng nhập, cập nhật realtime sau khi gọi startListening. */
    public LiveData<UserProfile> getCurrentProfile() {
        return currentProfile;
    }

    /** true nếu đã tải được hồ sơ và trạng thái là Banned. Chưa tải xong thì trả false. */
    public boolean isBanned() {
        UserProfile profile = currentProfile.getValue();
        return profile != null && profile.isBanned();
    }

    public boolean isAdmin() {
        UserProfile profile = currentProfile.getValue();
        return profile != null && profile.isAdmin();
    }

    /** Xóa hồ sơ đang giữ trong bộ nhớ khi đăng xuất/đổi tài khoản. */
    public void clearCurrentProfile() {
        stopListening();
        currentProfile.setValue(null);
    }

    /**
     * Đảm bảo users/{uid} tồn tại. Hồ sơ mới luôn bắt đầu với role = User.
     * Quyền Admin phải được cấp riêng trên Firestore bởi người có thẩm quyền.
     */
    public void ensureUserProfile(@NonNull final FirebaseUser user,
                                  @NonNull final UserCallback callback) {
        final DocumentReference ref = db.collection(FirestoreNames.USERS).document(user.getUid());
        ref.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        if (doc.exists()) {
                            UserProfile existing = doc.toObject(UserProfile.class);
                            if (existing != null) {
                                existing.setId(user.getUid());
                                callback.onSuccess(existing);
                                return;
                            }
                        }
                        createProfile(user, ref, callback);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
    }

    private void createProfile(@NonNull final FirebaseUser user,
                               @NonNull DocumentReference ref,
                               @NonNull final UserCallback callback) {
        String provider;
        if (user.isAnonymous()) {
            provider = FirestoreNames.AuthProvider.GUEST;
        } else if (user.getEmail() != null) {
            provider = FirestoreNames.AuthProvider.EMAIL;
        } else {
            provider = FirestoreNames.AuthProvider.GOOGLE;
        }

        final UserProfile profile = new UserProfile(defaultDisplayName(user), provider);
        // UserProfile constructor sets role = User. Never promote based on email.
        ref.set(profile)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        profile.setId(user.getUid());
                        callback.onSuccess(profile);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
    }

    private String defaultDisplayName(@NonNull FirebaseUser user) {
        String name = user.getDisplayName();
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        String uid = user.getUid();
        String suffix = uid.length() >= 4 ? uid.substring(uid.length() - 4) : uid;
        return "Khách " + suffix;
    }

    /** Ghi/cập nhật thiết bị hiện tại: user_devices/{uid}_{deviceId}. Lỗi chỉ log, không chặn app. */
    public void registerDevice(@NonNull Context context, @NonNull String uid) {
        String deviceId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = "unknown";
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("deviceId", deviceId);
        data.put("lastLogin", FieldValue.serverTimestamp());
        // merge để sau này thêm fcmToken không bị ghi đè
        db.collection(FirestoreNames.USER_DEVICES).document(uid + "_" + deviceId)
                .set(data, SetOptions.merge())
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Không ghi được user_devices", e);
                    }
                });
    }

    /** Đọc một lần hồ sơ của người bất kỳ (dùng để hiện tên và điểm uy tín của người đăng báo cáo). */
    public void fetchProfile(@NonNull String uid, @NonNull final UserCallback callback) {
        db.collection(FirestoreNames.USERS).document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        UserProfile profile = doc.exists() ? doc.toObject(UserProfile.class) : null;
                        if (profile != null) {
                            callback.onSuccess(profile);
                        } else {
                            callback.onError(new IllegalStateException("Không có hồ sơ users/" + doc.getId()));
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
    }

    /** Bắt đầu nghe users/{uid}. Gọi lại với uid khác sẽ thay listener cũ. */
    public void startListening(@NonNull String uid) {
        stopListening();
        currentProfile.setValue(null);
        profileListener = db.collection(FirestoreNames.USERS).document(uid)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.w(TAG, "Lỗi nghe users/{uid}", error);
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            UserProfile profile = snapshot.toObject(UserProfile.class);
                            if (profile != null) {
                                profile.setId(snapshot.getId());
                            }
                            currentProfile.setValue(profile);
                        } else {
                            currentProfile.setValue(null);
                        }
                    }
                });
    }

    public void stopListening() {
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
    }

    /** Sau khi liên kết Google: cập nhật authProvider và tên hiển thị. */
    public void markLinkedWithGoogle(@NonNull FirebaseUser user,
                                     @NonNull final DoneCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("authProvider", FirestoreNames.AuthProvider.GOOGLE);
        String name = user.getDisplayName();
        if (name != null && !name.trim().isEmpty()) {
            data.put("displayName", name);
        }
        db.collection(FirestoreNames.USERS).document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        callback.onDone();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
    }

    /** Đếm số báo cáo của người dùng (đếm trên server, không tải document về). */
    public void countMyReports(@NonNull String uid, @NonNull final CountCallback callback) {
        db.collection(FirestoreNames.REPORTS)
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", FirestoreNames.ReportStatus.ACTIVE)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(new OnSuccessListener<AggregateQuerySnapshot>() {
                    @Override
                    public void onSuccess(AggregateQuerySnapshot snapshot) {
                        callback.onCount(snapshot.getCount());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
    }
}
