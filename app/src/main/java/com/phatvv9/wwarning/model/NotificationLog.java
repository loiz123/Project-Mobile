package com.phatvv9.wwarning.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class NotificationLog {
    private String userId;
    private String reportId; // có thể null
    private String message;
    private boolean isInteracted;
    private Timestamp createdAt;

    public NotificationLog() {
        // Cần thiết cho Firestore deserialization
    }

    public NotificationLog(String userId, String reportId, String message, boolean isInteracted, Timestamp createdAt) {
        this.userId = userId;
        this.reportId = reportId;
        this.message = message;
        this.isInteracted = isInteracted;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Firestore bỏ tiền tố "is" của getter boolean; @PropertyName giữ đúng tên trường "isInteracted" theo schema
    @PropertyName("isInteracted")
    public boolean isInteracted() {
        return isInteracted;
    }

    @PropertyName("isInteracted")
    public void setInteracted(boolean interacted) {
        this.isInteracted = interacted;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}