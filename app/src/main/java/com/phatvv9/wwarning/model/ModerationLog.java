package com.phatvv9.wwarning.model;

import com.google.firebase.Timestamp;

public class ModerationLog {
    private String adminId;
    private String reportId;
    private String action; // Hide, Delete, Restore
    private Timestamp createdAt;

    public ModerationLog() {
        // Cần thiết cho Firestore deserialization
    }

    public ModerationLog(String adminId, String reportId, String action, Timestamp createdAt) {
        this.adminId = adminId;
        this.reportId = reportId;
        this.action = action;
        this.createdAt = createdAt;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}