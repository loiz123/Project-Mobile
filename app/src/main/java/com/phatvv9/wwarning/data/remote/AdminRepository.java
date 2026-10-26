package com.phatvv9.wwarning.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
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
     * Khóa tài khoản: Dùng WriteBatch để vừa ghi vào banned_users, vừa đổi status users thành "Banned"
     */
    public void banUser(final String userId, String adminId, String reason, @Nullable final AdminCallback callback) {
        DocumentReference banRef = db.collection("banned_users").document(userId);
        DocumentReference userRef = db.collection("users").document(userId);

        Map<String, Object> banData = new HashMap<>();
        banData.put("userId", userId);
        banData.put("bannedBy", adminId);
        banData.put("reason", reason);
        banData.put("bannedAt", Timestamp.now());

        WriteBatch batch = db.batch();
        batch.set(banRef, banData);
        batch.update(userRef, "status", "Banned");
        batch.commit()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
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
     * Mở khóa tài khoản: Dùng WriteBatch để vừa xóa khỏi banned_users, vừa đổi status users về "Active"
     */
    public void unbanUser(final String userId, @Nullable final AdminCallback callback) {
        DocumentReference banRef = db.collection("banned_users").document(userId);
        DocumentReference userRef = db.collection("users").document(userId);

        WriteBatch batch = db.batch();
        batch.delete(banRef);
        batch.update(userRef, "status", "Active");
        batch.commit()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
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
     * Lấy danh sách toàn bộ người dùng đang bị khóa tài khoản
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