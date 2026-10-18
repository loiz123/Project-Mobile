# Dữ liệu mẫu Firestore

Tạo các document sau trong Firebase Console trước khi demo. Các ID cần ổn định để các repository tham chiếu.

## `categories`
| Document ID | name | isRisk | isActive |
|---|---|---:|---:|
| sunny | Nắng | false | true |
| rain | Mưa | false | true |
| fog | Sương mù | false | true |
| storm | Bão | true | true |
| flood | Ngập lụt | true | true |
| fallen_tree | Cây đổ | true | true |

## `app_settings`
| Document ID | key | value |
|---|---|---:|
| reminder_minutes | reminder_minutes | 15 |
| expire_minutes | expire_minutes | 60 |
| radius_km | radius_km | 10 |
| downvote_threshold | downvote_threshold | 5 |

`severities` cần có ít nhất một document cho từng mức độ mà giao diện chọn mức độ sử dụng. Thời gian tạo/cập nhật nên lưu bằng Firestore Timestamp.
