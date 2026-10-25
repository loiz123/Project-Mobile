## Mô tả
Hoàn thiện schema Room Local Database, cập nhật các POJO Models chuẩn theo `docs/firestore-schema.md` (tên trường camelCase), cập nhật dữ liệu mẫu (Seed Data) cho Firestore, thiết lập Navigation Component cho ứng dụng và dọn dẹp cấu hình build.

## Task liên quan
Closes #1

## Người làm / Người review
- Người làm: @haiho2365
- Người review: @loiz123 @hntrhunter @dinhquoc03032626

## Những thay đổi chính
- **POJO Models & Firestore Data**:
  - `Category`: Đổi sang camelCase (`iconUrl`, `isRisk`, `isActive`), loại bỏ trường `severity_level` không có trong schema. Thêm `@PropertyName("isRisk")` và `@PropertyName("isActive")` cho getter/setter để Firestore không bị lưu thành `risk` / `active`.
  - `Severity`: Đổi `category_id` thành `categoryId`.
  - `AppSetting`: Thêm `@DocumentId` cho `key`, đổi `updated_at` thành `updatedAt` (`@ServerTimestamp`).
  - `seed-data.md`: Cập nhật bảng dữ liệu mẫu cấu hình hệ thống Firestore khớp với tên trường camelCase mới.
- **Build Configuration**:
  - Loại bỏ khối `compileOptions` Java 11 trùng lặp trong `app/build.gradle`, giữ lại cấu hình Java 17.
- **Navigation Component & UI Framework**:
  - Thiết lập `BottomNavigationView` liên kết với `NavHostFragment` và `nav_graph.xml`.
  - Tạo 3 Fragment khung tạm: `MapFragment`, `ProfileFragment`, `AdminFragment`.
  - Thiết lập ẩn tab Admin cố định (`adminItem.setVisible(false)`) trong `MainActivity`.

## Cách kiểm tra
1. Mở ứng dụng trên Emulator / Thiết bị thật.
2. Kiểm tra thanh Navigation phía dưới chuyển đổi mượt mà giữa Bản đồ và Hồ sơ.
3. Chạy `gradle assembleDebug` để đảm bảo không gặp lỗi biên dịch.

Kết quả mong đợi: Khung giao diện khởi chạy thành công với Bottom Navigation, tab Admin tạm ẩn, build không có cảnh báo/lỗi Java version conflict.

## Ghi chú & Kế hoạch Merge
- **Các Fragment tạm**: 3 Fragment (`MapFragment`, `ProfileFragment`, `AdminFragment`) là các khung tạm để Thiện, Phát, Quốc có thể trực tiếp sửa và phát triển giao diện/nghiệp vụ vào các file này.
- **Tab Admin**: Tạm thời set `setVisible(false)` cố định trong `MainActivity.java`. Sau này khi tích hợp module User Auth sẽ kết nối kiểm tra điều kiện với `users.role`.
- **Kế hoạch Merge Branch**: Chờ Phát merge nhánh `setup/firebase` vào `develop`. Sau đó sẽ merge `develop` về nhánh `hai`, giữ file `build.gradle` của `develop` và chỉ bổ sung thêm Room, Navigation (không sửa plugin `google-services` và Firebase BoM).

## Ảnh chụp / video ngắn
- Ảnh chụp màn hình giao diện Bottom Navigation: `docs/screenshots/bottom_navigation.png`

## Checklist
- [x] Chạy được trên emulator, không lỗi build
- [x] Không commit local.properties, API key, file keystore
- [x] Tên trường đúng theo docs/firestore-schema.md
- [x] Không sửa file của thành viên khác (hoặc đã báo họ)
- [x] Có ít nhất 1 người approve trước khi merge

## Câu hỏi cho người review
- Về phần POJO models đã chuẩn camelCase và annotation `@PropertyName`, mọi người thấy đã hoàn toàn sẵn sàng để đấu nối với Firestore chưa?
