# Wwarning
# 🌦️ WWarning - Ứng dụng Cảnh báo Thời tiết Thời gian thực

WWarning là ứng dụng di động chia sẻ thông tin thời tiết dựa trên nguồn dữ liệu cộng đồng (Crowdsourced) hoàn toàn theo thời gian thực. Không phụ thuộc vào các API dự báo thời tiết truyền thống có độ trễ cao, WWarning trao quyền cho người dùng tự cập nhật và cảnh báo các hiện tượng thời tiết (Nắng, Mưa, Bão, Ngập lụt) tại khu vực của họ. 

## 🚀 Các chức năng cốt lõi (Core Features)

1. Báo cáo siêu tốc (Quick Report): 
   Giao diện chạm nhanh cho phép người dùng gửi trạng thái thời tiết tại vị trí hiện tại lên hệ thống trong vòng dưới 3 giây.
2. Bản đồ Live 100% (Real-time Map): 
   Sử dụng Firestore realtime listener (snapshot listener) và công nghệ phân vùng không gian (Geohashing), bản đồ tự động hiển thị ngay lập tức các marker cảnh báo mới từ những người dùng khác trong bán kính 10km mà không cần tải lại trang.
3. Nhắc nhở thông minh 15 phút (15-Min Reminder): 
   Hệ thống tự động kích hoạt đồng hồ đếm ngược và đẩy Push Notification đúng 15 phút sau khi người dùng báo cáo, yêu cầu cập nhật lại diễn biến thời tiết (Ví dụ: "Trời đã tạnh mưa chưa?").
4. Kiểm duyệt bởi cộng đồng (Community Validation): 
   Người dùng đi ngang qua có thể "Đồng tình" (Upvote) để làm nổi bật cảnh báo thật, hoặc "Báo cáo sai" (Downvote) để hệ thống tự động gỡ bỏ các marker chứa tin giả, spam.
5. Định danh ẩn danh (Anonymous Access): 
   Sử dụng ứng dụng và đóng góp báo cáo ngay lập tức thông qua Device ID (Mã thiết bị) mà không bắt buộc phải qua các bước đăng ký tài khoản rườm rà.
6. Tự động dọn dẹp dữ liệu (Auto-Expire): 
   Các báo cáo thời tiết sẽ tự động bị ẩn hoặc làm mờ sau một khoảng thời gian nhất định (ví dụ: 60 phút) để đảm bảo bản đồ luôn phản ánh đúng tình trạng thực tế nhất.
7. Bộ lọc rủi ro (Risk Filter): 
   Thanh công cụ cho phép người dùng lọc bản đồ để chỉ hiển thị các yếu tố cản trở giao thông cực đoan như "Ngập lụt" hay "Cây đổ".
8. Hệ thống Điểm uy tín (Reputation System): 
   Thu thập điểm uy tín dựa trên độ chính xác của các báo cáo được cộng đồng xác nhận, giúp lọc ra những người dùng đóng góp chất lượng.

---

## 👥 Đội ngũ phát triển (Team Members)

Dự án được xây dựng và thiết kế bởi 4 thành viên, mỗi người chịu trách nhiệm cho các module nghiệp vụ và cơ sở dữ liệu riêng biệt:

* Trần Đại Phát - @loiz123
* Hà Ngọc Thiện - @hntrhunter
* Tiêu Lâm Định Quốc - @dinhquoc03032626
* Hồ Duy Hải - @haiho2365-source

## ⚙️ Cài đặt để build

1. Mở dự án bằng Android Studio (nó tự tạo `local.properties` chứa `sdk.dir`; file này nằm trong `.gitignore`, **không commit**). Không cần điền `MAPS_API_KEY`: key Maps đã nằm trong `AndroidManifest.xml` và đã được giới hạn theo package `com.phatvv9.wwarning` + SHA-1 của `app/debug.keystore`.
2. Keystore dùng chung `app/debug.keystore` đã được commit và `app/build.gradle` ký bản debug bằng chính nó. Mọi máy build ra cùng SHA-1 (`2E:CC:84:5F:EC:D3:99:B5:29:09:F6:F2:2D:B1:4B:5E:89:63:24:CE`), nên bản đồ và đăng nhập Google chạy ngay sau khi pull. Đây là keystore **debug** (mật khẩu mặc định `android`), không dùng để phát hành.
3. `google-services.json` đã có sẵn trong `app/`.
4. Dán `firestore.rules` vào Firebase Console → Firestore → Rules → Publish.
5. Tài khoản Admin: đăng nhập app một lần, rồi vào Firestore sửa `users/{uid}.role` thành `Admin` (rules không cho app tự nâng quyền).
