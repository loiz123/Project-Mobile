package com.phatvv9.wwarning.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminRepository {
    private final FirebaseFirestore db;

    public AdminRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public AdminRepository(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

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
                        // Cập nhật trạng thái trong profile người dùng
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

    public interface AdminCallback {
        void onSuccess();
        void onError(String error);
    }
}
