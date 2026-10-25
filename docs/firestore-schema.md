# Firestore schema - WWarning

 Sơ đồ ERD: `docs/erd.png` (mã nguồn Mermaid: `docs/erd.mmd`).

## 1. Quy ước

- Tên trường dùng **camelCase** (`createdAt`, `upCount`, `isRisk`); tên collection dùng snake_case số nhiều.
- Id document dùng `@DocumentId` trong POJO, không lưu thành trường.
- Khóa ngoại là trường `string` chứa id document (`userId`, `reportId`...).
- Thời gian dùng `Timestamp`, ghi bằng `@ServerTimestamp`.
- Giá trị liệt kê lưu `string`, viết hoa chữ đầu (`Active`, `Hidden`).
- Trường boolean `isRisk`, `isActive`, `isInteracted` phải có `@PropertyName("isRisk")` ở cả getter và setter, nếu không Firestore lưu thành `risk`, `active`, `interacted`.
- Ký hiệu `?` ở cột Kiểu: có thể trống.
- Không dùng lambda trong code Firebase; viết listener bằng class ẩn danh.

## 2. Phân công

| # | Collection | Id document | Phụ trách | Ai ghi | Ai đọc |
|---|---|---|---|---|---|
| 1 | `users` | uid Firebase Auth | Phát | Phát (tạo khi đăng nhập, cập nhật điểm); Quốc (đổi `status` khi khóa) | Tất cả |
| 2 | `user_devices` | `{uid}_{deviceId}` | Phát | Phát (mỗi lần đăng nhập) | Quốc (gửi thông báo) |
| 3 | `categories` | tự sinh | Hải | Hải (nhập dữ liệu mẫu) | Thiện, Quốc |
| 4 | `severities` | tự sinh | Hải | Hải (nhập dữ liệu mẫu) | Thiện |
| 5 | `reports` | tự sinh | Thiện | Thiện (tạo báo cáo); Quốc (`upCount`, `downCount`, `status`) | Tất cả |
| 6 | `votes` | `{reportId}_{uid}` | Quốc | Quốc | Quốc |
| 7 | `reminders` | tự sinh | Quốc | Quốc | Quốc |
| 8 | `notifications` | tự sinh | Quốc | Quốc | Quốc |
| 9 | `reputation_logs` | tự sinh | Phát | Phát (hàm `updateReputation`, Quốc gọi khi vote) | Phát (Hồ sơ) |
| 10 | `banned_users` | uid người bị khóa | Quốc | Quốc (màn Admin) | Quốc, Phát (kiểm tra khi đăng nhập) |
| 11 | `moderation_logs` | tự sinh | Quốc | Quốc (màn Admin) | Quốc |
| 12 | `app_settings` | tên thông số | Hải | Hải (nhập thủ công) | Thiện, Quốc |

Tổng số bảng mỗi người: Phát 3, Thiện 1, Quốc 5, Hải 3.

## 3. Chi tiết các collection

### 3.1 `users` (Phát)

| Trường | Kiểu | Mặc định / ghi chú |
|---|---|---|
| displayName | string | Tài khoản ẩn danh dùng "Khách" + 4 ký tự cuối uid |
| authProvider | string | `Guest` hoặc `Google` |
| reputationScore | number (int) | 100; chỉ đổi qua `updateReputation` bằng transaction |
| reportCount | number (int) | 0; số báo cáo đã đăng, hiện ở Hồ sơ |
| role | string | `User` hoặc `Admin` |
| status | string | `Active` hoặc `Banned` |
| createdAt | Timestamp | |

### 3.2 `user_devices` (Phát)

| Trường | Kiểu | Ghi chú |
|---|---|---|
| userId | string | FK -> users |
| deviceId | string | |
| fcmToken | string? | |
| lastLogin | Timestamp | Cập nhật mỗi lần đăng nhập |

### 3.3 `categories` (Hải)

| Trường | Kiểu | Ghi chú |
|---|---|---|
| name | string | Nắng, Mưa, Sương mù, Bão, Ngập lụt, Cây đổ |
| iconUrl | string | |
| isRisk | boolean | true cho Ngập lụt, Bão, Cây đổ (bộ lọc rủi ro) |
| isActive | boolean | true |

### 3.4 `severities` (Hải)

| Trường | Kiểu | Ghi chú |
|---|---|---|
| categoryId | string | FK -> categories |
| name | string | Ví dụ "Mưa lất phất", "Mưa rào" |
| level | number (int) | 1 là nhẹ nhất |

### 3.5 `reports` (Thiện)

| Trường | Kiểu | Ghi chú |
|---|---|---|
| userId | string | FK -> users, người đăng |
| categoryId | string | FK -> categories |
| severityId | string? | FK -> severities |
| latitude | number (double) | |
| longitude | number (double) | |
| geohash | string | Tạo bằng GeoFireUtils, dùng truy vấn vùng 10 km |
| status | string | `Active`, `Hidden`, `Deleted` (xóa mềm) |
| upCount | number (int) | 0 |
| downCount | number (int) | 0 |
| createdAt | Timestamp | |
| expiresAt | Timestamp | `createdAt` + `expire_minutes` của app_settings |

### 3.6 `votes` (Quốc)

Id = `{reportId}_{uid}`, nên mỗi người chỉ vote một lần cho mỗi báo cáo.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| reportId | string | FK -> reports |
| userId | string | FK -> users |
| voteValue | number (int) | `1` hoặc `-1` |
| createdAt | Timestamp | |

### 3.7 `reminders` (Quốc)

| Trường | Kiểu | Ghi chú |
|---|---|---|
| userId | string | FK -> users |
| reportId | string | FK -> reports |
| status | string | `Pending`, `Triggered`, `Cancelled` |
| triggerAt | Timestamp | `createdAt` + `reminder_minutes` |
| createdAt | Timestamp | |

### 3.8 `notifications` (Quốc, POJO `NotificationLog`)

| Trường | Kiểu | Ghi chú |
|---|---|---|
| userId | string | FK -> users |
| reportId | string? | FK -> reports |
| message | string | |
| isInteracted | boolean | true nếu người dùng bấm vào thông báo |
| createdAt | Timestamp | |

### 3.9 `reputation_logs` (Phát)

| Trường | Kiểu | Ghi chú |
|---|---|---|
| userId | string | FK -> users, người được cộng hoặc trừ điểm |
| reportId | string? | FK -> reports |
| delta | number (int) | Ví dụ `1`, `-2` |
| reason | string | Ví dụ "Được upvote" |
| createdAt | Timestamp | |

### 3.10 `banned_users` (Quốc)

Id = uid người bị khóa. Khi khóa, đồng thời đặt `users.status = Banned`.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| userId | string | FK -> users, trùng id document |
| bannedBy | string | FK -> users, admin thực hiện |
| reason | string | |
| bannedAt | Timestamp | |
| expiresAt | Timestamp? | Trống nếu khóa vĩnh viễn |

### 3.11 `moderation_logs` (Quốc)

| Trường | Kiểu | Ghi chú |
|---|---|---|
| adminId | string | FK -> users |
| reportId | string | FK -> reports |
| action | string | `Hide`, `Delete`, `Restore` |
| createdAt | Timestamp | |

### 3.12 `app_settings` (Hải)

Mỗi thông số là một document, id = tên thông số. Đọc cả collection một lần rồi tra theo id.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| key | string | `@DocumentId`, không lưu thành trường |
| value | string | Phải parse sang số khi dùng |
| description | string | |
| updatedAt | Timestamp | |

Dữ liệu mẫu:

| Id document | value | Dùng ở |
|---|---|---|
| `reminder_minutes` | 15 | Nhắc nhở (Quốc) |
| `expire_minutes` | 60 | `reports.expiresAt` (Thiện) |
| `radius_km` | 10 | Truy vấn geohash (Thiện) |
| `downvote_threshold` | 5 | Quy tắc tự ẩn báo cáo (Quốc) |

Nơi đọc thông số phải có giá trị mặc định trong code (15, 60, 10, 5) để app vẫn chạy khi chưa có dữ liệu hoặc đọc lỗi.