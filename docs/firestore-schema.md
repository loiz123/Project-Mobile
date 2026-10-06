# Cấu trúc cơ sở dữ liệu Firestore (WWarning)

Tài liệu này ghi chú các trường dữ liệu (Schema) của các collection trong ứng dụng, phục vụ cho việc thống nhất kiểu dữ liệu giữa các thành viên trong nhóm.

---

## Phụ trách: Ngọc Thiện (@hntrhunter)

### 1. Collection: `weather_reports`
Lưu trữ thông tin cảnh báo thời tiết do người dùng cộng đồng báo cáo lên.

| Tên trường | Kiểu dữ liệu (Firestore) | Mô tả |
| :--- | :--- | :--- |
| `id` | String | Mã định danh duy nhất của báo cáo (Document ID). |
| `userId` | String (Reference) | ID của người đăng báo cáo (tham chiếu từ collection `users`). |
| `categoryId` | String (Reference) | ID của danh mục thời tiết (nắng, mưa, bão... tham chiếu từ `weather_categories`). |
| `severityId` | String (Reference) | ID mức độ nghiêm trọng (nhẹ, vừa, nguy hiểm... tham chiếu từ `report_severities`). |
| `latitude` | Number (Double) | Vĩ độ của địa điểm được báo cáo. |
| `longitude` | Number (Double) | Kinh độ của địa điểm được báo cáo. |
| `geohash` | String | Chuỗi mã hoá tọa độ (Geohash) để truy vấn theo bán kính khu vực nhanh chóng. |
| `description` | String | Mô tả chi tiết/ghi chú của người dùng về tình trạng thời tiết. |
| `createdAt` | Timestamp | Thời gian báo cáo được tạo. |
| `expiresAt` | Timestamp | Thời gian báo cáo hết hạn (bằng `createdAt` + số phút cấu hình trong `app_settings`). |
| `upvotes` | Number (Integer) | Số lượt người dùng vote đồng tình báo cáo này là đúng. |
| `downvotes` | Number (Integer) | Số lượt người dùng vote báo cáo này là sai/spam. |
| `status` | String | Trạng thái của báo cáo (ví dụ: `active`, `expired`, `resolved`). |

---

### 2. Collection: `report_media`
Lưu trữ các file hình ảnh/video đính kèm theo báo cáo thời tiết.

| Tên trường | Kiểu dữ liệu (Firestore) | Mô tả |
| :--- | :--- | :--- |
| `id` | String | Mã định danh duy nhất của media (Document ID). |
| `reportId` | String (Reference) | ID của báo cáo chứa ảnh/video này (tham chiếu từ `weather_reports`). |
| `mediaUrl` | String | Đường dẫn tải file (URL) lưu trên Firebase Storage. |
| `mediaType` | String | Loại file (ví dụ: `image`, `video`). |
| `createdAt` | Timestamp | Thời gian upload file. |
