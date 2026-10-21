package com.phatvv9.wwarning.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.phatvv9.wwarning.model.BannedUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminRepository {
    private final FirebaseFirestore db;

    public AdminRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public AdminRepository(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Khóa tài khoản người dùng: ghi vào collection banned_users và đổi status users thành Banned
     */
    public void banUser(final String userId, String adminId, String reason, @Nullable final AdminCallback callback) {
        Map<String, Object> banData = new HashMap<>();
        banData.put("userId", userId);
        banData.put("bannedBy", adminId);
        banData.put("reason", reason);
        banData.put("bannedAt", Timestamp.now());

        db.collection("banned_users").document(userId)
                .set(banData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        db.collection("users").document(userId).update("status", "Banned");
                        if (callback != null) callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (callback != null) callback.onError(e.getMessage());
                    }
                });
    }

    /**
     * Mở khóa tài khoản: xóa khỏi banned_users và đổi status users thành Active
     */
    public void unbanUser(final String userId, @Nullable final AdminCallback callback) {
        db.collection("banned_users").document(userId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        db.collection("users").document(userId).update("status", "Active");
                        if (callback != null) callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (callback != null) callback.onError(e.getMessage());
                    }
                });
    }

    /**
     * Lấy danh sách toàn bộ người dùng đang bị khóa
     */
    public void getBannedUsers(@NonNull final BannedUsersCallback callback) {
        db.collection("banned_users")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<BannedUser> list = new ArrayList<>();
                        if (queryDocumentSnapshots != null) {
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                BannedUser bannedUser = doc.toObject(BannedUser.class);
                                if (bannedUser != null) {
                                    bannedUser.setUserId(doc.getId());
                                    list.add(bannedUser);
                                }
                            }
                        }
                        callback.onSuccess(list);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    public interface AdminCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface BannedUsersCallback {
        void onSuccess(@NonNull List<BannedUser> bannedUsers);
        void onError(String error);
    }
}