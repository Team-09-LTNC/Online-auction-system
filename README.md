# 🏷️ Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)
## 📂 Cấu Trúc Thư Mục 

```text
Online-auction-system/
├── pom.xml                                 # Cấu hình Maven (Gson, JavaFX, MySQL Driver, JUnit)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/auction/
│   │   │       │
│   │   │       ├── common/                 # 1. TẦNG DÙNG CHUNG (Giao thức & Dữ liệu)
│   │   │       │   ├── dto/                # Data Transfer Object (Đóng gói dữ liệu mạng)
│   │   │       │   ├── exception/          # Custom Exceptions (VD: InvalidBidException)
│   │   │       │   ├── model/              # Các Entity cốt lõi (Mô hình hóa OOP)
│   │   │       │   │   ├── bid/            # Lớp xử lý giao dịch (BidTransaction)
│   │   │       │   │   ├── entity/         # Lớp cơ sở (BaseEntity abstract class)
│   │   │       │   │   ├── item/           # Sản phẩm (Item, Electronics, Art...)
│   │   │       │   │   ├── user/           # Người dùng (User, Bidder, Seller, Admin)
│   │   │       │   │   └── AuctionStatus.java # Enum trạng thái phiên đấu giá
│   │   │       │   └── observer/           # Interface cho Observer Pattern
│   │   │       │
│   │   │       ├── server/                 # 2. TẦNG SERVER (Xử lý đa luồng & CSDL)
│   │   │       │   ├── dao/                # Data Access Object (Truy vấn CSDL an toàn)
│   │   │       │   ├── manager/            # Business Logic (AuctionManager)
│   │   │       │   ├── network/            # Quản lý mạng (NetworkManager, ClientHandler)
│   │   │       │   ├── utils/              # Tiện ích (DatabaseConnection Singleton)
│   │   │       │   └── ServerApplication.java # Điểm khởi chạy Server
│   │   │       │
│   │   │       └── client/                 # 3. TẦNG CLIENT (Giao diện JavaFX)
│   │   │           ├── controller/         # Điều khiển UI (Gọi Platform.runLater)
│   │   │           ├── network/            # SocketClient (Giao tiếp với Server)
│   │   │           ├── util/               # Tiện ích UI (AlertBox, Formatter)
│   │   │           └── ClientApplication.java # Điểm khởi chạy Client
│   │   │
│   │   └── resources/                      # 4. TÀI NGUYÊN TĨNH
│   │       ├── css/                        # Style giao diện
│   │       ├── fxml/                       # Giao diện SceneBuilder
│   │       └── images/                     # Hình ảnh, icon
│   │
│   └── test/                               # 5. TẦNG KIỂM THỬ (JUnit 5)
│       └── java/
│           └── com/auction/server/manager/AuctionManagerTest.java 
└── README.md
```text

## 📂 Chi Tiết Kiến Trúc Thư Mục (dùng để tham khảo)


### 1. TẦNG COMMON (`com.auction.common`)
*Vùng lõi quy định chuẩn giao tiếp. Tuyệt đối KHÔNG chứa logic giao diện (JavaFX) hay truy vấn DB (JDBC).*

com.auction.common/
├── dto/                               # Data Transfer Objects (Gói tin JSON)
│   ├── Request.java                   # Lớp cha trừu tượng cho mọi yêu cầu gửi lên Server.
│   ├── Response.java                  # Lớp cha trừu tượng cho mọi phản hồi từ Server.
│   ├── LoginRequest.java              # Chứa thông tin đăng nhập.
│   ├── BidRequest.java                # Chứa thông tin đặt giá (auctionId, amount).
│   └── AuctionUpdateDTO.java          # Server dùng để đẩy giá mới về Client (Real-time).
├── model/                             # Thực thể nghiệp vụ (Entities)
│   ├── entity/
│   │   └── Entity.java                # Lớp cơ sở chứa ID dùng chung.
│   ├── user/
│   │   ├── User.java (Abstract)       # Người dùng chung.
│   │   ├── Bidder.java / Seller.java  # Các vai trò cụ thể.
│   ├── item/
│   │   ├── Item.java (Abstract)       # Sản phẩm đấu giá.
│   │   ├── ItemFactory.java           # Factory Pattern để khởi tạo linh hoạt các loại hàng hóa.
│   └── bid/
│       ├── Auction.java               # Đối tượng quản lý trạng thái phiên đấu giá hiện tại.
│       ├── BidTransaction.java        # Bản ghi lịch sử một lần đặt giá cụ thể.
│       └── AuctionStatus.java         # Enum định nghĩa: OPEN, RUNNING, FINISHED.
├── observer/                          # Hỗ trợ Real-time
│   └── AuctionObserver.java           # Interface định nghĩa hàm onNewBidReceived().
└── exception/                         # Ngoại lệ tùy chỉnh
└── InvalidBidException.java       # Ném ra khi giá đặt thấp hơn giá hiện tại.

### 2. TẦNG SERVER (`com.auction.server`)
*Não bộ của hệ thống. Quản lý Concurrency, xử lý thuật toán Anti-sniping và duy trì an toàn cơ sở dữ liệu.*

com.auction.server/
├── ServerApplication.java             # Điểm khởi chạy hệ thống, mở Port Socket.
├── network/                           # Quản lý kết nối mạng đa luồng
│   ├── ServerManager.java             # Vòng lặp chấp nhận kết nối (ServerSocket.accept).
│   └── ClientHandler.java             # Thực thi Runnable và AuctionObserver để giao tiếp với 1 Client.
├── manager/                           # Tầng điều phối (Services - Singleton)
│   ├── AuctionManager.java            # CHỐNG RACE CONDITION: Dùng synchronized(auction) trên từng phiên.
│   ├── UserManager.java               # Quản lý người dùng trực tuyến (online users).
│   └── ProductManager.java            # Quản lý việc đăng và duyệt sản phẩm.
├── dao/                               # Data Access Objects (Truy vấn CSDL)
│   ├── UserDao.java                   # Sử dụng PreparedStatement để truy vấn người dùng.
│   ├── AuctionDao.java                # Cập nhật giá cao nhất và thời gian kết thúc.
│   └── BidTransactionDao.java         # Dùng DATABASE TRANSACTION (Commit/Rollback) để lưu lịch sử giá.
└── utils/                             # Công cụ hỗ trợ
└── DatabaseConnection.java        # Tích hợp HIKARICP để quản lý Pool kết nối hiệu quả cao.

### 3. TẦNG CLIENT (`com.auction.client`)

com.auction.client/
├── ClientApplication.java             # Khởi chạy giao diện JavaFX (extends Application).
├── network/                           # Giao tiếp mạng phía Client
│   ├── SocketClient.java              # Singleton quản lý 1 kết nối duy nhất tới Server.
│   └── ServerListenerThread.java      # Luồng ngầm liên tục đọc JSON từ Server đẩy về.
├── controller/                        # Điều khiển giao diện (MVC)
│   ├── LoginController.java           # Xử lý sự kiện đăng nhập.
│   ├── HomeController.java            # Hiển thị danh sách phiên đấu giá.
│   └── BidRoomController.java         # THREAD-SAFETY: Dùng Platform.runLater() để cập nhật giá.
└── util/                              # Tiện ích UI
├── UIHelper.java                  # Hàm chuyển đổi các Scene (màn hình) FXML.
└── Formatter.java                 # Định dạng hiển thị tiền tệ (VND) và thời gian.

### 4. TÀI NGUYÊN & KIỂM THỬ (`resources` & `test`)

src/
├── main/resources/
│   ├── fxml/                          # Chứa toàn bộ file giao diện (.fxml).
│   ├── css/                           # Định dạng màu sắc, hiệu ứng (.css).
│   └── images/                        # Logo, icon, ảnh sản phẩm.
└── test/java/com/auction/             # JUnit 5
└── manager/
└── AuctionManagerTest.java    # Kiểm tra kịch bản đặt giá đồng thời (Concurrency Test).