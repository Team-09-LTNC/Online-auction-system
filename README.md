# 🔨 Online Auction System - Team 09 (UET)

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=java" />
  <img src="https://img.shields.io/badge/JavaFX-17-blue?style=for-the-badge&logo=javafx" />
  <img src="https://img.shields.io/badge/Architecture-Client--Server-green?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Build-Maven-red?style=for-the-badge&logo=apache-maven" />
</p>

---

## 📖 Giới thiệu dự án
Dự án được phát triển cho môn **Lập trình nâng cao** tại Đại học Công nghệ. Hệ thống cho phép nhiều người dùng tham gia đấu giá sản phẩm theo thời gian thực, áp dụng các nguyên lý **OOP** và mô hình **MVC**.

## 👥 Thành viên nhóm
| STT | Họ và tên |
| :--- | :--- |
| 1 | **Nguyễn Trung Hiếu**  |
| 2 | **Phạm Việt Hoàng** |
| 3 | **Vũ Gia Khánh** |
| 4 | **Trương Bảo Kiên** |

---

## 🏗 Kiến trúc Hệ thống
Hệ thống tuân thủ mô hình phân tầng để tách biệt giao diện, nghiệp vụ và dữ liệu:
* **Kiến trúc:** Client-Server kết nối qua Socket (JSON data).
* **Client-side:** JavaFX + FXML áp dụng mô hình MVC.
* **Server-side:** Controller - Model - DAO (Chỉ Server có quyền truy cập Database).

---

## 🛠 Chức năng chính (Roadmap 10 Tuần)

### 🟢 Chức năng bắt buộc
- [ ] **Quản lý người dùng:** Đăng ký/Đăng nhập (Bidder, Seller, Admin).
- [ ] **Quản lý sản phẩm:** Seller đăng tải, chỉnh sửa thông tin sản phẩm đấu giá.
- [ ] **Tham gia đấu giá:** Đặt giá Real-time, cập nhật người dẫn đầu tức thì.
- [ ] **Kết thúc phiên:** Tự động xác định người thắng và đóng phiên khi hết giờ.
- [ ] **Xử lý ngoại lệ:** Chống đặt giá sai logic hoặc thao tác khi phiên đã đóng.

### 🟡 Chức năng nâng cao (Target điểm 10+)
- [ ] **Concurrent Bidding:** Xử lý đấu giá đồng thời an toàn (tránh Race Condition).
- [ ] **Realtime Update:** Sử dụng Observer Pattern để đồng bộ giá cho tất cả Client.
- [ ] **Auto-Bidding:** Hệ thống tự động trả giá thay người dùng dựa trên mức tối đa.
- [ ] **Anti-sniping:** Tự động gia hạn phiên nếu có bid mới ở những giây cuối.

---

## 📐 Áp dụng Design Patterns
Dự án dự kiến áp dụng các mẫu thiết kế chuẩn để tối ưu mã nguồn:
* **Singleton:** Quản lý kết nối Database tập trung.
* **Factory Method:** Khởi tạo linh hoạt các loại sản phẩm khác nhau.
* **Observer:** Cập nhật biến động giá thầu đến toàn bộ Client ngay lập tức.

---

## 📜 Quy định làm việc (Git Flow)
* **Commit thường xuyên:** Minh chứng tiến độ làm bài cá nhân (bắt buộc).
* **Conventional Commits:** Sử dụng tiền tố `feat:`, `fix:`, `docs:`, `test:`.
* **Chất lượng mã:** Tuân thủ Google Java Style Guide và viết Unit Test (JUnit).

---

## 💻 Hướng dẫn khởi chạy
1. **Yêu cầu:** Java 17+, Maven.
2. **Clone:** `git clone [URL_DỰ_ÁN]`
3. **Chạy:** Khởi động `ServerApp` trước, sau đó mở các phiên `ClientApp`.
