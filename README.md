# 🔨 Online Auction System - Team 09 - UET

Dự án Bài tập lớn môn **Lập trình nâng cao**. [cite_start]Hệ thống cho phép nhiều người dùng tham gia đấu giá trực tuyến theo thời gian thực, áp dụng các nguyên lý thiết kế hướng đối tượng (OOP) và kiến trúc Client-Server[cite: 8, 9, 125].

## 👥 Thành viên nhóm & Phân công
* [cite_start]**Trương Bảo Kiên** (@trbk07) [cite: 116, 129, 141].
* [cite_start]**Thành viên 2** (@username) [cite: 63, 101, 128].
* [cite_start]**Thành viên 3** (@username) [cite: 34, 38, 129].
* [cite_start]**Thành viên 4** (@username) [cite: 83, 136, 147].

---

## 🏗 Kiến trúc & Công nghệ (Tech Stack)
[cite_start]Hệ thống được thiết kế theo mô hình phân tầng để tách biệt giao diện, nghiệp vụ và dữ liệu[cite: 123, 124]:
* [cite_start]**Kiến trúc:** Client-Server (Giao tiếp qua Socket/JSON)[cite: 125, 126].
* [cite_start]**Giao diện (Client):** JavaFX + FXML áp dụng mô hình MVC[cite: 63, 128].
* [cite_start]**Xử lý (Server):** Java Controller-Model-DAO[cite: 129].
* [cite_start]**Công cụ xây dựng:** Maven / Gradle[cite: 133].
* [cite_start]**Kiểm thử:** JUnit cho các logic nghiệp vụ quan trọng[cite: 136].

---

## 🛠 Các Chức năng Chính (Lộ trình 10 Tuần)

### [cite_start]1. Nhóm chức năng bắt buộc [cite: 28, 147]
- [ ] [cite_start]**Quản lý người dùng:** Đăng ký/đăng nhập với 3 vai trò: Bidder, Seller, Admin[cite: 31, 33, 34].
- [ ] [cite_start]**Quản lý sản phẩm:** Seller thêm/sửa/xóa sản phẩm; hiển thị thông tin giá, thời gian[cite: 38, 40, 45].
- [ ] [cite_start]**Tham gia đấu giá:** Đặt giá realtime, kiểm tra tính hợp lệ và cập nhật người dẫn đầu[cite: 46, 47, 50].
- [ ] [cite_start]**Kết thúc phiên:** Tự động đóng phiên khi hết giờ, xác định người thắng và chuyển trạng thái (OPEN -> RUNNING -> FINISHED)[cite: 51, 52, 55].
- [ ] [cite_start]**Xử lý ngoại lệ:** Chặn đặt giá thấp hơn giá hiện tại hoặc khi phiên đã đóng[cite: 56, 58, 59].

### [cite_start]2. Nhóm chức năng nâng cao (Target điểm 10+) [cite: 69, 147]
- [ ] [cite_start]**Concurrent Bidding:** Xử lý tranh chấp khi nhiều người cùng bid (Tránh Lost Update/Race Condition)[cite: 83, 147].
- [ ] [cite_start]**Auto-Bidding:** Hệ thống tự động trả giá thay người dùng dựa trên `maxBid` và `increment`[cite: 72, 74].
- [ ] [cite_start]**Anti-sniping:** Tự động gia hạn phiên đấu giá nếu có bid mới trong những giây cuối[cite: 89, 90].
- [ ] [cite_start]**Realtime Price Curve:** Biểu đồ đường hiển thị biến động giá theo thời gian thực[cite: 101, 102].

---

## [cite_start]📐 Áp dụng OOP & Design Patterns [cite: 107, 139]
[cite_start]Dự án tuân thủ nghiêm ngặt các nguyên tắc hướng đối tượng và các mẫu thiết kế[cite: 118, 147]:
* [cite_start]**Encapsulation:** Sử dụng private/protected và getter/setter[cite: 119].
* [cite_start]**Polymorphism:** Override các phương thức hiển thị thông tin sản phẩm[cite: 121].
* [cite_start]**Singleton:** Quản lý kết nối Database và Auction Manager[cite: 141].
* [cite_start]**Factory Method:** Khởi tạo linh hoạt các loại sản phẩm (Electronics, Art, Vehicle)[cite: 142].
* [cite_start]**Observer:** Cập nhật biến động giá thầu đến toàn bộ Client ngay lập tức[cite: 143].

---

## 📜 Quy định đóng góp (Git Flow)
* [cite_start]**Commit thường xuyên:** Minh chứng tiến độ làm bài (Không chấp nhận một commit duy nhất cuối kỳ)[cite: 19, 20].
* [cite_start]**Conventional Commits:** Ví dụ `feat: add auto-bidding logic`, `fix: resolve race condition in bid transaction`[cite: 137].
* [cite_start]**Review:** Mọi code mới phải thông qua Pull Request trên GitHub[cite: 17].

---

## 💻 Hướng dẫn chạy thử
1. Clone repo: `git clone [URL_DỰ_ÁN]`
2. [cite_start]Mở bằng IntelliJ/Eclipse (hỗ trợ Maven)[cite: 133].
3. [cite_start]Chạy Server trước, sau đó khởi chạy các Client[cite: 130].
