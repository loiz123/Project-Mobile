package com.phatvv9.wwarning.data.remote;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminRepository {
    private final FirebaseFirestore db;

    public AdminRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void banUser(String userId, String reason, AdminCallback callback) {
        Map<String, Object> banData = new HashMap<>();
        banData.put("userId", userId);
        banData.put("reason", reason);
        banData.put("bannedAt", Timestamp.now());

        db.collection("banned_users").document(userId)
                .set(banData)
                .addOnSuccessListener(aVoid -> {
                    // Cập nhật trạng thái trong profile người dùng
                    db.collection("users").document(userId).update("status", "Banned");
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void unbanUser(String userId, AdminCallback callback) {
        db.collection("banned_users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(userId).update("status", "Active");
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public interface AdminCallback {
        void onSuccess();
        void onError(String error);
    }
}