package com.phatvv9.wwarning.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reports")
public class ReportEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String categoryId;
    private double latitude;
    private double longitude;
    private String geohash;
    private String status;
    private int upCount;
    private int downCount;
    private long createdAt;
    private long expiresAt;

    public ReportEntity(@NonNull String id, String userId, String categoryId, double latitude, double longitude,
                        String geohash, String status, int upCount, int downCount, long createdAt, long expiresAt) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geohash = geohash;
        this.status = status;
        this.upCount = upCount;
        this.downCount = downCount;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
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
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
}