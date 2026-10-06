package com.phatvv9.wwarning.model;

import com.google.firebase.Timestamp;

public class ReportMedia {
    private String id;
    private String reportId;
    private String mediaUrl;
    private String mediaType;
    private Timestamp createdAt;

    public ReportMedia() {}

    public ReportMedia(String id, String reportId, String mediaUrl, String mediaType, Timestamp createdAt) {
        this.id = id;
        this.reportId = reportId;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
