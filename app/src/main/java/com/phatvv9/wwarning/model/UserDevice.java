package com.phatvv9.wwarning.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/** user_devices/{uid}_{deviceId}: thiết bị đã đăng nhập (dùng cho FCM về sau). */
public class UserDevice {
    @DocumentId
    private String id;
    private String userId;
    private String deviceId;
    private String fcmToken;   // null nếu chưa dùng FCM
    private Timestamp lastLogin;

    public UserDevice() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public Timestamp getLastLogin() { return lastLogin; }
    public void setLastLogin(Timestamp lastLogin) { this.lastLogin = lastLogin; }
}
