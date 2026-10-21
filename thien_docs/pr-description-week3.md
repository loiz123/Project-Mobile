## Mô tả
Hoàn thành tính năng Tuần 3 cho module Bản đồ và Geohash:
1. **Truy vấn Geohash 10km:** Tích hợp `GeoFireUtils.getGeoHashQueryBounds` tính toán các dải geohash bao quanh vị trí GPS người dùng (bán kính 10km). Lọc lại khoảng cách thực tế chính xác bằng `GeoFireUtils.getDistanceBetween`.
2. **Lọc báo cáo quá hạn & Tự động làm mờ (Auto-expire & Marker fading):**
   - Lọc bỏ triệt để các báo cáo đã quá hạn (`expiresAt <= now`) và các báo cáo bị ẩn (`status != "Active"`).
   - Marker sắp hết hạn (còn dưới 15 phút): tự động làm mờ với `alpha = 0.5f` và gắn nhãn "(Sắp hết hạn)".
3. **Marker uy tín cao (High Reputation Marker):**
   - Báo cáo có `upCount >= 3` được hiển thị nổi bật với biểu tượng ⭐, `zIndex = 2.0f`, snippet ghi nhận sự xác thực từ cộng đồng.
4. **Giữ góc nhìn bản đồ khi xoay máy (Preserve Camera State):**
   - Lưu toạ độ camera và mức zoom qua `onSaveInstanceState`.
   - Phục hồi mượt mà trong `onMapReady` và không để GPS tự động zoom đè lên góc nhìn người dùng đã chọn.

## Task liên quan
Closes #4

## Người làm / Người review
- Người làm: @hntrhunter
- Người review: @haiho2365-source @loiz123

## Những thay đổi chính
- `GeoUtils.java`: Bổ sung hàm tiện ích `getGeoHashQueryBounds()` và `getDistanceBetween()`.
- `ReportRepository.java`: Hỗ trợ `setLocation(lat, lng, radiusKm)` truy vấn song song các dải geohash bounds, loại bỏ tin hết hạn và lọc khoảng cách thực tế. Dùng Anonymous Class theo quy chuẩn.
- `MapFragment.java`: Cập nhật toạ độ GPS cho repository, thiết lập hiệu ứng alpha 0.5f cho marker sắp hết hạn, đánh dấu nổi bật bài nhiều upvote, lưu/phục hồi vị trí camera qua `onSaveInstanceState`.

## Cách kiểm tra
1. Mở ứng dụng, cấp quyền vị trí, bản đồ zoom về vị trí hiện tại.
2. Xoay ngang màn hình máy ảo: góc nhìn bản đồ và các marker được giữ nguyên vẹn.
3. Tạo báo cáo mới: marker hiển thị rõ nét (alpha = 1.0f).
4. Kiểm tra các marker cũ đã quá 60 phút sẽ tự động biến mất khỏi bản đồ.

## Checklist
- [x] Chạy được trên emulator, không lỗi build
- [x] Không commit local.properties, API key, file keystore
- [x] Tên trường đúng theo docs/firestore-schema.md
- [x] Không sửa file của thành viên khác (hoặc đã báo họ)
- [x] Có ít nhất 1 người approve trước khi merge
