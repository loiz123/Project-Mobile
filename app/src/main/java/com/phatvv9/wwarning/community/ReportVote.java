package com.phatvv9.wwarning.community; // Thay đúng tên package của bạn nếu khác

public class ReportVote {
    private String voteId;
    private String reportId;
    private String userId;
    private int voteType; // +1: Upvote, -1: Downvote
    private long timestamp;

    // Constructor rỗng bắt buộc cho Firestore
    public ReportVote() {}

    public ReportVote(String voteId, String reportId, String userId, int voteType, long timestamp) {
        this.voteId = voteId;
        this.reportId = reportId;
        this.userId = userId;
        this.voteType = voteType;
        this.timestamp = timestamp;
    }

    public String getVoteId() { return voteId; }
    public void setVoteId(String voteId) { this.voteId = voteId; }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getVoteType() { return voteType; }
    public void setVoteType(int voteType) { this.voteType = voteType; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}