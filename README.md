# 🏷️ Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)
## 📂 Cấu Trúc Thư Mục 
 `dev_client` và `dev_server` phải giữ cùng cấu trúc thư mục với branch `dev`, test ở `dev` oke rồi thì mới merge vào `main`

- `dev_client`: chỉ sửa folder `client` và `resources` làm giao diện
- `dev_server`: chỉ sửa folder `server`
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
