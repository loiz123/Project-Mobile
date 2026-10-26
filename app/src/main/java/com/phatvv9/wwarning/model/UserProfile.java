package com.phatvv9.wwarning.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.phatvv9.wwarning.util.FirestoreNames;

/** users/{uid}: hồ sơ người dùng. Document id chính là FirebaseUser.getUid(). */
public class UserProfile {
    @DocumentId
    private String id;
    private String displayName;
    private String authProvider;   // Guest, Google
    private String role;           // User, Admin
    private String status;         // Active, Banned
    private int reputationScore;
    private int reportCount = 0;
    @ServerTimestamp
    private Timestamp createdAt;

    /** Constructor rỗng bắt buộc cho Firestore. */
    public UserProfile() {}

    public UserProfile(String displayName, String authProvider) {
        this.displayName = displayName;
        this.authProvider = authProvider;
        this.role = FirestoreNames.Role.USER;
        this.status = FirestoreNames.UserStatus.ACTIVE;
        this.reputationScore = 100;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getReputationScore() { return reputationScore; }
    public void setReputationScore(int reputationScore) { this.reputationScore = reputationScore; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    // @Exclude: nếu không có, Firestore sẽ ghi thêm trường "banned"/"admin" vào document.
    @Exclude
    public boolean isBanned() {
        return FirestoreNames.UserStatus.BANNED.equals(status);
    }

    @Exclude
    public boolean isAdmin() {
        return FirestoreNames.Role.ADMIN.equals(role);
    }
}
