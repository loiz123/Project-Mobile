package com.phatvv9.wwarning.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

/** reputation_logs/{autoId}: lịch sử cộng/trừ điểm uy tín (dùng ở tuần 3). */
public class ReputationLog {
    @DocumentId
    private String id;
    private String userId;     // người được cộng/trừ điểm
    private String reportId;   // báo cáo liên quan, có thể null
    private int delta;         // +5, -10...
    private String reason;
    @ServerTimestamp
    private Timestamp createdAt;

    public ReputationLog() {}

    public ReputationLog(String userId, String reportId, int delta, String reason) {
        this.userId = userId;
        this.reportId = reportId;
        this.delta = delta;
        this.reason = reason;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public int getDelta() { return delta; }
    public void setDelta(int delta) { this.delta = delta; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
