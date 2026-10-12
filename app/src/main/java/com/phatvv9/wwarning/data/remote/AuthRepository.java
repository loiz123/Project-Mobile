package com.phatvv9.wwarning.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class AuthRepository {

    /** Kết quả bất đồng bộ của thao tác đăng nhập. */
    public interface AuthCallback {
        void onSuccess(@NonNull FirebaseUser user);

        void onError(@NonNull Exception e);
    }

    private final FirebaseAuth auth;

    public AuthRepository() {
        this(FirebaseAuth.getInstance());
    }

    /** Constructor cho test, truyền FirebaseAuth giả. */
    AuthRepository(@NonNull FirebaseAuth auth) {
        this.auth = auth;
    }

    /** Người dùng hiện tại, null nếu chưa đăng nhập. */
    @Nullable
    public FirebaseUser currentUser() {
        return auth.getCurrentUser();
    }

    /** Tiện ích: uid hiện tại, null nếu chưa đăng nhập. */
    @Nullable
    public String currentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /** true nếu đang đăng nhập ẩn danh (chưa liên kết Google). */
    public boolean isAnonymous() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null && user.isAnonymous();
    }

    /**
     * Đăng nhập ẩn danh. Nếu máy đã có phiên đăng nhập thì dùng lại,
     * không tạo uid mới (tránh mỗi lần mở app ra một người dùng khác).
     */
    public void signInAnonymously(@NonNull final AuthCallback callback) {
        FirebaseUser existing = auth.getCurrentUser();
        if (existing != null) {
            callback.onSuccess(existing);
            return;
        }
        auth.signInAnonymously()
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        FirebaseUser user = result.getUser();
                        if (user != null) {
                            callback.onSuccess(user);
                        } else {
                            callback.onError(new IllegalStateException(
                                    "Đăng nhập ẩn danh không trả về user"));
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

    /** Đăng xuất (dùng khi test hoặc đổi tài khoản). */
    public void signOut() {
        auth.signOut();
    }
}
