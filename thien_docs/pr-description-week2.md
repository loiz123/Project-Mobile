## Mô tả
Hoàn thành tính năng Tuần 2 cho module Bản đồ và Báo cáo nhanh: tích hợp Dialog báo cáo nhanh (QuickReportDialog) với 6 loại thời tiết chuẩn theo `docs/seed-data.md`, tính toán chuỗi geohash phục vụ truy vấn không gian qua `GeoUtils`, xử lý tầng dữ liệu `ReportRepository` kết nối Firestore (lắng nghe snapshot realtime và tạo báo cáo mới), cập nhật marker trực tiếp trên Google Maps kèm nhãn thời gian tương đối ("Vừa xong", "10 phút trước").

## Task liên quan
Closes #2

## Người làm / Người review
- Người làm: @hntrhunter
- Người review: @haiho2365-source @loiz123

## Những thay đổi chính
- `ReportRepository.java`:
  - Lắng nghe realtime snapshot từ collection `reports` (lọc `status == "Active"`) trả về `LiveData<List<Report>>`.
  - Hàm `createReport()` tự động sinh Geohash, gán `userId` (từ Firebase Auth), `expiresAt` (+60 phút mặc định), lưu vào Firestore.
  - Tuân thủ quy ước nhóm: dùng anonymous class (không lambda), chuẩn camelCase theo `docs/firestore-schema.md`.
- `GeoUtils.java`:
  - Hàm tiện ích `getGeoHash(lat, lng)` sử dụng thư viện `GeoFireUtils` chuẩn hóa tọa độ.
- `QuickReportDialog.java` & `dialog_quick_report.xml`:
  - Giao diện dạng dialog chạm nhanh 6 nút hiện tượng thời tiết (`cat_nang`, `cat_mua`, `cat_suong`, `cat_bao`, `cat_ngap`, `cat_cay`).
  - Gửi báo cáo dưới 3 giây, hiển thị Toast phản hồi và tự động đóng dialog.
- `MapFragment.java` & `fragment_map.xml`:
  - Bổ sung nút FloatingActionButton (`fabQuickReport`) mở hộp thoại báo cáo nhanh.
  - Quan sát `ReportRepository.getReports()` để render marker tương ứng trên bản đồ.
  - Hiển thị nhãn thời gian tương đối trên snippet marker và đổi màu marker theo từng loại hiện tượng thời tiết.

## Cách kiểm tra
1. Mở ứng dụng, cấp quyền vị trí, bản đồ zoom về vị trí hiện tại.
2. Bấm nút dấu cộng (+) ở góc dưới bên phải màn hình để mở dialog báo cáo nhanh.
3. Chọn một hiện tượng thời tiết (ví dụ: Mưa, Nắng).
4. Kiểm tra thông báo Toast thành công và marker xuất hiện ngay lập tức trên bản đồ tại vị trí hiện tại.
5. Chạm vào marker để xem tiêu đề loại thời tiết và nhãn thời gian ("Vừa xong").

Kết quả mong đợi: Tạo báo cáo thành công lên Firestore `reports`, marker realtime cập nhật mượt mà không cần reload trang.

## Checklist
- [x] Chạy được trên emulator, không lỗi build
- [x] Không commit local.properties, API key, file keystore
- [x] Tên trường đúng theo docs/firestore-schema.md
- [x] Không sửa file của thành viên khác (hoặc đã báo họ)
- [x] Có ít nhất 1 người approve trước khi merge

## Câu hỏi cho người review
- Về màu sắc marker và thời gian hết hạn mặc định (60 phút), nhóm có cần điều chỉnh gì thêm trước khi bước sang Tuần 3 (truy vấn bán kính 10km geohash bounds)?
