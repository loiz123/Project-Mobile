package com.phatvv9.wwarning.model;

import com.google.firebase.Timestamp;

public class Report {
    private String id;
    private String userId;
    private String categoryId;
    private String severityId;
    private double latitude;
    private double longitude;
    private String geohash;
    private String description;
    private Timestamp createdAt;
    private Timestamp expiresAt;
    private int upvotes;
    private int downvotes;
    private String status;

    public Report() {
        // Default constructor required for calls to DataSnapshot.getValue(Report.class)
    }

    public Report(String id, String userId, String categoryId, String severityId, double latitude, double longitude, String geohash, String description, Timestamp createdAt, Timestamp expiresAt) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.severityId = severityId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geohash = geohash;
        this.description = description;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.upvotes = 0;
        this.downvotes = 0;
        this.status = "active";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getSeverityId() { return severityId; }
    public void setSeverityId(String severityId) { this.severityId = severityId; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getGeohash() { return geohash; }
    public void setGeohash(String geohash) { this.geohash = geohash; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }
    public int getUpvotes() { return upvotes; }
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }
    public int getDownvotes() { return downvotes; }
    public void setDownvotes(int downvotes) { this.downvotes = downvotes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
