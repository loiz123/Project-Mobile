package com.phatvv9.wwarning.data.remote;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.phatvv9.wwarning.R;

public class AuthRepository {

    private static final String TAG = "WWarning";

    /** Kết quả bất đồng bộ của thao tác đăng nhập. */
    public interface AuthCallback {
        void onSuccess(@NonNull FirebaseUser user);

        void onError(@NonNull Exception e);
    }

    /** Kết quả đăng nhập/liên kết Google. */
    public interface GoogleCallback {
        /**
         * @param switchedAccount true nếu tài khoản Google này đã có sẵn bên uid khác,
         *                        app đã chuyển sang uid đó (báo cáo ẩn danh cũ nằm ở uid cũ).
         */
        void onSuccess(@NonNull FirebaseUser user, boolean switchedAccount);

        void onCancelled();

        void onError(@NonNull String message);
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

    /**
     * Đăng nhập bằng Email và Password (dành cho Quản trị viên Admin).
     * Xác thực thông tin tài khoản admin và cấp quyền quản trị viên.
     */
    public void signInWithEmailAndPassword(@NonNull final String email,
                                           @NonNull final String password,
                                           @NonNull final AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        FirebaseUser user = result.getUser();
                        if (user != null) {
                            callback.onSuccess(user);
                        } else {
                            callback.onError(new IllegalStateException(
                                    "Đăng nhập không trả về user"));
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

    /**
     * Mở hộp thoại chọn tài khoản Google (Credential Manager), rồi:
     * - đang ẩn danh: liên kết Google vào uid hiện tại (giữ nguyên uid, báo cáo cũ vẫn là của bạn);
     * - Google đó đã gắn với uid khác: đăng nhập vào uid đó.
     */
    public void signInWithGoogle(@NonNull Activity activity, @NonNull final GoogleCallback callback) {
        CredentialManager credentialManager = CredentialManager.create(activity);
        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(
                activity.getString(R.string.default_web_client_id)).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();

        credentialManager.getCredentialAsync(
                activity,
                request,
                null,
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        handleCredential(response.getCredential(), callback);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.w(TAG, "Credential Manager lỗi", e);
                        if (e instanceof GetCredentialCancellationException) {
                            callback.onCancelled();
                        } else if (e instanceof NoCredentialException) {
                            callback.onError("Máy chưa có tài khoản Google nào để đăng nhập");
                        } else {
                            callback.onError("Không đăng nhập được Google: " + e.getMessage());
                        }
                    }
                });
    }

    private void handleCredential(@NonNull Credential credential,
                                  @NonNull GoogleCallback callback) {
        if (credential instanceof CustomCredential
                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                .equals(credential.getType())) {
            try {
                GoogleIdTokenCredential google =
                        GoogleIdTokenCredential.createFrom(credential.getData());
                firebaseAuthWithGoogle(google.getIdToken(), callback);
            } catch (Exception e) {
                Log.w(TAG, "Không đọc được Google ID token", e);
                callback.onError("Không đọc được thông tin tài khoản Google");
            }
        } else {
            callback.onError("Loại thông tin đăng nhập không được hỗ trợ");
        }
    }

    private void firebaseAuthWithGoogle(@NonNull String idToken,
                                        @NonNull final GoogleCallback callback) {
        final AuthCredential authCredential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseUser current = auth.getCurrentUser();

        if (current != null && current.isAnonymous()) {
            current.linkWithCredential(authCredential)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult result) {
                            FirebaseUser user = result.getUser();
                            if (user != null) {
                                callback.onSuccess(user, false);
                            } else {
                                callback.onError("Liên kết xong nhưng không có user");
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (e instanceof FirebaseAuthUserCollisionException) {
                                // Google này đã gắn với uid khác: đăng nhập vào uid đó
                                AuthCredential updated =
                                        ((FirebaseAuthUserCollisionException) e).getUpdatedCredential();
                                signInWithCredential(updated != null ? updated : authCredential,
                                        true, callback);
                            } else {
                                callback.onError("Liên kết Google lỗi: " + e.getMessage());
                            }
                        }
                    });
        } else {
            signInWithCredential(authCredential, false, callback);
        }
    }

    private void signInWithCredential(@NonNull AuthCredential credential,
                                      final boolean switched,
                                      @NonNull final GoogleCallback callback) {
        auth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        FirebaseUser user = result.getUser();
                        if (user != null) {
                            callback.onSuccess(user, switched);
                        } else {
                            callback.onError("Đăng nhập xong nhưng không có user");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError("Đăng nhập Google lỗi: " + e.getMessage());
                    }
                });
    }

    /** Đăng xuất (dùng khi test hoặc đổi tài khoản). */
    public void signOut() {
        auth.signOut();
    }
}
