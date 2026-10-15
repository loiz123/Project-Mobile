package com.phatvv9.wwarning.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class ReportVote {
    @DocumentId
    private String voteId;
    private String reportId;
    private String userId;
    private int voteValue; // 1: Upvote, -1: Downvote
    private Timestamp createdAt;

    public ReportVote() {
        // Cần thiết cho Firestore deserialization
    }

    public ReportVote(String voteId, String reportId, String userId, int voteValue, Timestamp createdAt) {
        this.voteId = voteId;
        this.reportId = reportId;
        this.userId = userId;
        this.voteValue = voteValue;
        this.createdAt = createdAt;
    }

    public String getVoteId() {
        return voteId;
    }

    public void setVoteId(String voteId) {
        this.voteId = voteId;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getVoteValue() {
        return voteValue;
    }

    public void setVoteValue(int voteValue) {
        this.voteValue = voteValue;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}