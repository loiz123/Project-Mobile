# WWarning — Firestore schema (12 collections)

Schema này bám theo bảng 4 trong `WWarning_KeHoach_DoAn_12bang.docx`. Tên collection là tên triển khai Firebase (cột Collection trong kế hoạch), dùng camelCase cho field và Timestamp cho thời gian. Các trường `userId`, `reportId`, `categoryId`, `severityId`, `bannedBy`, `adminId` là ID document tham chiếu, không phải khóa ngoại SQL.

## 1. `users/{uid}` — Phát
- `displayName`: string
- `email`: string, tùy chọn
- `photoUrl`: string, tùy chọn
- `reputationScore`: number/int, mặc định `100`
- `role`: string, mặc định `User` (giá trị quản trị là `Admin`)
- `status`: string, mặc định `Active`; tài khoản bị khóa dùng `Banned`
- `createdAt`: Timestamp
- `lastLogin`: Timestamp

Document ID chính là Firebase Auth UID.

## 2. `user_devices/{uid}_{deviceId}` — Phát
- `userId`: string (UID)
- `deviceId`: string (Android ID)
- `fcmToken`: string, tùy chọn khi Firebase Messaging chưa cấp token
- `lastLogin`: Timestamp
- `notificationsEnabled`: boolean

## 3. `categories/{categoryId}` — theo ERD: weather_categories
- `name`: string; `isRisk`: boolean; `isActive`: boolean; `iconUrl`: string tùy chọn

Dữ liệu mẫu: Nắng, Mưa, Sương mù, Bão, Ngập lụt, Cây đổ.

## 4. `severities/{severityId}` — theo ERD: report_severities
- `categoryId`: string; `name`: string; `description`: string tùy chọn; `level`: number; `isActive`: boolean

## 5. `reports/{reportId}` — theo ERD: weather_reports
- `userId`: string; `categoryId`: string; `severityId`: string hoặc null
- `latitude`: number; `longitude`: number; `geohash`: string
- `status`: string (`Active`, `Hidden`, ...)
- `upCount`: number/int; `downCount`: number/int
- `createdAt`: Timestamp; `expiresAt`: Timestamp

## 6. `votes/{reportId}_{uid}` — theo ERD: report_votes
- `reportId`: string; `userId`: string; `voteValue`: number (`1` hoặc `-1`); `createdAt`: Timestamp

## 7. `reminders/{reminderId}`
- `userId`: string; `reportId`: string; `status`: string (`Pending`, `Triggered`, `Cancelled`); `scheduledAt`: Timestamp; `createdAt`: Timestamp

## 8. `notifications/{notificationId}`
- `userId`: string; `reminderId`: string tùy chọn; `title`: string; `body`: string; `isInteracted`: boolean; `createdAt`: Timestamp

## 9. `reputation_logs/{logId}` — Phát
- `userId`: string; `reportId`: string hoặc null; `delta`: number/int; `reason`: string; `createdAt`: Timestamp

## 10. `banned_users/{uid}`
- `userId`: string; `bannedBy`: string (admin UID); `reason`: string; `bannedAt`: Timestamp; `unbanAt`: Timestamp hoặc null

## 11. `moderation_logs/{logId}`
- `adminId`: string; `reportId`: string; `action`: string; `reason`: string tùy chọn; `createdAt`: Timestamp

## 12. `app_settings/{key}`
Document ID là tên thông số: `reminder_minutes`, `expire_minutes`, `radius_km`, `downvote_threshold`.
- `key`: string; `value`: number/string/boolean theo thiết lập; `updatedAt`: Timestamp tùy chọn

## Quy tắc tích hợp
- Không tạo `password_hash`: Firebase Authentication quản lý xác thực.
- Không tạo `active_sessions`: Firestore snapshot listener thay cho WebSocket.
- `users/{uid}` là nguồn trạng thái tài khoản chính. Không cho client tự sửa `role`, `status` hoặc `reputationScore`.
- 12 collection ở trên tương ứng 12 bảng ERD; một số tên hiển thị trong ERD khác tên collection triển khai, như bảng mapping đã ghi.
- `schema.sql` được gửi kèm trong cuộc trò chuyện là schema MySQL cho website bất động sản `batdongsan`, không phải schema Firebase của WWarning; không import SQL đó vào Firestore.
