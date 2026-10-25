# Bảng Kết Quả Kiểm Thử Chế Độ Offline (Tuần 3 - Hải)

## 🎯 Mục Tiêu Kiểm Thử
Kiểm tra khả năng hoạt động ngoại tuyến (Offline) của ứng dụng khi người dùng ngắt kết nối mạng hoặc bật chế độ máy bay (Airplane mode).

---

## 📋 Kịch Bản Kiểm Thử & Kết Quả

| STT | Kịch Bản Kiểm Thử | Trạng Thái Mạng | Hành Động | Kết Quả Mong Đợi | Kết Quả Thực Tế | Trạng Thái |
|:---:|:---|:---:|:---|:---|:---|:---:|
| 1 | **Tải dữ liệu ban đầu** | Online | Mở app lần đầu có kết nối Wi-Fi/4G. | Dữ liệu báo cáo & categories từ Firestore được tải và tự động lưu vào Room DB. | Đã tải và lưu thành công vào Room DB (`reports`, `categories`). | PASS |
| 2 | **Chuyển sang Offline** | Offline | Bật chế độ máy bay (Airplane Mode). | App nhận diện mất mạng qua `ConnectivityManager.NetworkCallback` / `SyncManager`. | `SyncManager` phát hiện mất mạng và cập nhật `isOnlineLiveData = false`. | PASS |
| 3 | **Hiển thị Thanh Báo Offline** | Offline | Quan sát giao diện bản đồ (`MapFragment`). | Thanh thông báo màu đỏ `"⚠️ Đang offline - Đang xem dữ liệu đã lưu"` xuất hiện ở đầu màn hình. | Thanh banner `tvOfflineBanner` hiện đúng thông báo. | PASS |
| 4 | **Đọc Dữ Liệu Offline** | Offline | Mở lại ứng dụng hoặc xoay màn hình khi mất mạng. | Bản đồ vẫn hiển thị toàn bộ các Marker báo cáo đã tải trước đó từ Room DB. | `ReportLocalCache` truy vấn Room DB và trả về danh sách marker cũ trên bản đồ. | PASS |
| 5 | **Bộ Lọc Rủi Ro Offline** | Offline | Bấm chọn Chip `"⚠️ Chỉ rủi ro"` trên `ChipGroup`. | Lọc đúng các loại báo cáo rủi ro cao (Ngập lụt, Bão, Cây đổ) từ dữ liệu Room. | Bản đồ chỉ hiển thị các marker thuộc nhóm rủi ro khi offline. | PASS |
| 6 | **Khôi phục Online** | Online | Tắt chế độ máy bay, kết nối lại Internet. | Thanh báo offline tự động ẩn (`GONE`), app kết nối lại Firestore listener. | Banner ẩn, dữ liệu tự động đồng bộ realtime từ Firestore. | PASS |

---

## 📝 Kết Luận
Tính năng lưu đệm ngoại tuyến (Offline Cache), bộ lọc rủi ro (Risk Filter), và giám sát kết nối (`NetworkUtils` / `SyncManager`) hoạt động ổn định, đạt toàn bộ tiêu chí nghiệm thu Tuần 3.

- **Người thực hiện:** Hồ Duy Hải (@haiho2365-source)
- **Cập nhật:** 2026-10-23
