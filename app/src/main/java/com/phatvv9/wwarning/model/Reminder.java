package com.phatvv9.wwarning.model;

import com.google.firebase.Timestamp;

public class Reminder {
    private String userId;
    private String reportId;
    private String status; // Pending, Triggered, Cancelled
    private Timestamp triggerAt;
    private Timestamp createdAt;

    public Reminder() {
        // Cần thiết cho Firestore deserialization
    }

    public Reminder(String userId, String reportId, String status, Timestamp triggerAt, Timestamp createdAt) {
        this.userId = userId;
        this.reportId = reportId;
        this.status = status;
        this.triggerAt = triggerAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getTriggerAt() {
        return triggerAt;
    }

    public void setTriggerAt(Timestamp triggerAt) {
        this.triggerAt = triggerAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}