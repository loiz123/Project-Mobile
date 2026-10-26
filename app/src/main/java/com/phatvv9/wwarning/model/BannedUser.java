package com.phatvv9.wwarning.model;

import com.google.firebase.Timestamp;

public class BannedUser {
    private String userId;
    private String bannedBy;
    private String reason;
    private Timestamp bannedAt;
    private Timestamp expiresAt; // null nếu khóa vĩnh viễn

    public BannedUser() {
        // Cần thiết cho Firestore deserialization
    }

    public BannedUser(String userId, String bannedBy, String reason, Timestamp bannedAt, Timestamp expiresAt) {
        this.userId = userId;
        this.bannedBy = bannedBy;
        this.reason = reason;
        this.bannedAt = bannedAt;
        this.expiresAt = expiresAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBannedBy() {
        return bannedBy;
    }

    public void setBannedBy(String bannedBy) {
        this.bannedBy = bannedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Timestamp getBannedAt() {
        return bannedAt;
    }

    public void setBannedAt(Timestamp bannedAt) {
        this.bannedAt = bannedAt;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }
}