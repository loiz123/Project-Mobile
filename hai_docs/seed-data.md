# DỮ LIỆU MẪU CẤU HÌNH HỆ THỐNG (SEED DATA)
*Người thực hiện: Hồ Duy Hải (@haiho2365)*
*Dự án: WWarning - Project-Mobile*

## 1. Collection `categories` (Loại thời tiết)
| Document ID  | name       | iconUrl | isRisk | isActive |
|--------------|------------|---------|--------|----------|
| `cat_nang`   | Nắng       | ""      | false  | true     |
| `cat_mua`    | Mưa        | ""      | false  | true     |
| `cat_suong`  | Sương mù   | ""      | false  | true     |
| `cat_ngap`   | Ngập lụt   | ""      | true   | true     |
| `cat_cay`    | Cây đổ     | ""      | true   | true     |
| `cat_bao`    | Bão        | ""      | true   | true     |

## 2. Collection `app_settings` (Cấu hình ứng dụng)
| Key (Document ID)    | value | description                           |
|----------------------|-------|---------------------------------------|
| `reminder_minutes`   | "15"  | Số phút nhắc nhở cập nhật diễn biến   |
| `expire_minutes`     | "60"  | Số phút tự động làm mờ/ẩn báo cáo    |
| `radius_km`          | "10"  | Bán kính tìm kiếm báo cáo xung quanh  |
| `downvote_threshold` | "5"   | Ngưỡng downvote chênh lệch để ẩn bài  |

## 3. Collection `severities` (Mức độ nghiêm trọng)
| Document ID | categoryId  | name            | level |
|-------------|-------------|-----------------|-------|
| `sev_mua_1` | `cat_mua`   | Mưa lất phất    | 1     |
| `sev_mua_2` | `cat_mua`   | Mưa rào nặng hạt| 2     |
| `sev_ngap_1`| `cat_ngap`  | Ngập nửa bánh xe| 3     |
