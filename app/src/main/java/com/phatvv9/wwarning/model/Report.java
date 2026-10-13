package com.phatvv9.wwarning.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

// Collection "reports" - khớp docs/firestore-schema.md mục 3.5
public class Report {
    @DocumentId
    private String id;
    private String userId;
    private String categoryId;
    private String severityId; // có thể null
    private double latitude;
    private double longitude;
    private String geohash;
    private String status = "Active"; // Active, Hidden, Deleted
    private int upCount;
    private int downCount;
    @ServerTimestamp
    private Timestamp createdAt;
    private Timestamp expiresAt;

    // Constructor rỗng bắt buộc cho Firestore
    public Report() {}

    // createdAt do server điền; expiresAt = bây giờ + expire_minutes (tính ở phía gọi)
    public Report(String userId, String categoryId, String severityId,
                  double latitude, double longitude, String geohash, Timestamp expiresAt) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.severityId = severityId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geohash = geohash;
        this.expiresAt = expiresAt;
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getUpCount() { return upCount; }
    public void setUpCount(int upCount) { this.upCount = upCount; }
    public int getDownCount() { return downCount; }
    public void setDownCount(int downCount) { this.downCount = downCount; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }
}
