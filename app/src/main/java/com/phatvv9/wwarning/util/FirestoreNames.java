package com.phatvv9.wwarning.util;

/**
 * Tên collection và giá trị chuỗi dùng chung cho Firestore.
 * Mọi chỗ trong app dùng hằng số ở đây, không gõ lại chuỗi "reports", "Active"... để tránh sai chính tả.
 * Xem docs/firestore-naming.md.
 */
public final class FirestoreNames {

    private FirestoreNames() {}

    // ---- Tên collection (snake_case, số nhiều) ----
    public static final String USERS = "users";
    public static final String USER_DEVICES = "user_devices";
    public static final String CATEGORIES = "categories";
    public static final String SEVERITIES = "severities";
    public static final String REPORTS = "reports";
    public static final String VOTES = "votes";
    public static final String REMINDERS = "reminders";
    public static final String NOTIFICATIONS = "notifications";
    public static final String REPUTATION_LOGS = "reputation_logs";
    public static final String BANNED_USERS = "banned_users";
    public static final String MODERATION_LOGS = "moderation_logs";
    public static final String APP_SETTINGS = "app_settings";

    // ---- users.role ----
    public static final class Role {
        public static final String USER = "User";
        public static final String ADMIN = "Admin";
        private Role() {}
    }

    // ---- users.status ----
    public static final class UserStatus {
        public static final String ACTIVE = "Active";
        public static final String BANNED = "Banned";
        private UserStatus() {}
    }

    // ---- users.authProvider ----
    public static final class AuthProvider {
        public static final String GUEST = "Guest";
        public static final String GOOGLE = "Google";
        private AuthProvider() {}
    }

    // ---- reports.status ----
    public static final class ReportStatus {
        public static final String ACTIVE = "Active";
        public static final String HIDDEN = "Hidden";
        public static final String DELETED = "Deleted";
        private ReportStatus() {}
    }

    // ---- reminders.status ----
    public static final class ReminderStatus {
        public static final String PENDING = "Pending";
        public static final String TRIGGERED = "Triggered";
        public static final String CANCELLED = "Cancelled";
        private ReminderStatus() {}
    }

    // ---- moderation_logs.action ----
    public static final class ModerationAction {
        public static final String HIDE = "Hide";
        public static final String DELETE = "Delete";
        public static final String RESTORE = "Restore";
        private ModerationAction() {}
    }

    // ---- app_settings (document id = key) ----
    public static final class SettingKey {
        public static final String REMINDER_MINUTES = "reminder_minutes";
        public static final String EXPIRE_MINUTES = "expire_minutes";
        public static final String RADIUS_KM = "radius_km";
        public static final String DOWNVOTE_THRESHOLD = "downvote_threshold";
        private SettingKey() {}
    }
}
