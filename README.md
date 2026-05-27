# Online Auction System

Hệ thống đấu giá trực tuyến theo mô hình client-server, viết bằng Java 21 và JavaFX. Client giao tiếp với server bằng socket JSON; server xử lý nghiệp vụ, truy cập database qua DAO và đẩy sự kiện realtime ngược về các client đang theo dõi phiên đấu giá.

## Công Nghệ

- Java 21
- JavaFX 21 + FXML
- Maven
- Gson
- MySQL Connector/J + HikariCP
- H2 cho test integration
- JUnit 5 + AssertJ
- SLF4J + Logback

## Entry Points

| Thành phần | Class | Nhiệm vụ |
|---|---|---|
| Client | `com.auction.client.MainApp` | Khởi động JavaFX, load màn hình đăng nhập và stylesheet. |
| Server | `com.auction.server.ServerApp` | Khởi động socket server, chờ client kết nối. |

## Build Và Chạy JAR

Build 2 file JAR phát hành:

```bash
mvn clean package
```

Artifact sau khi build:

```text
target/MainApp.jar
target/ServerApp.jar
```

## Chạy Server Và Client Trên Cùng Một Máy

Nếu chạy cả `ServerApp.jar` và `MainApp.jar` trên cùng một máy, không cần truyền IP server.

Mở terminal thứ nhất và chạy server:

```bash
java -jar ServerApp.jar
```

Mở terminal thứ hai trên cùng máy đó và chạy client:

```bash
java -jar MainApp.jar
```

Trường hợp này app sẽ dùng mặc định `127.0.0.1:8080`, tức là kết nối về server đang chạy
trên chính máy hiện tại.

## Chạy Trên Nhiều Máy Cùng Wi-Fi/LAN

Chọn 1 máy làm server. Máy này sẽ chạy `ServerApp.jar` và các máy còn lại sẽ chạy
`MainApp.jar` để kết nối vào server.

### 1. Trên máy làm server

Lấy địa chỉ IPv4 của máy server:

```bash
ipconfig
```

Tìm dòng `IPv4 Address`, ví dụ máy server có IP là:

```text
192.168.1.15
```

Sau đó chạy server:

```bash
java -jar ServerApp.jar
```

Server mặc định lắng nghe ở port `8080`.

### 2. Trên các máy client

Các máy khác trong cùng Wi-Fi/LAN chạy `MainApp.jar` và truyền IP của máy server:

```bash
java -jar MainApp.jar --server-ip=192.168.1.15
```

Ví dụ: nếu máy chạy `ServerApp.jar` có IP là `192.168.1.15`, thì mọi máy client đều chạy:

```bash
java -jar MainApp.jar --server-ip=192.168.1.15
```

Có thể dùng cách tương đương bằng system property:

```bash
java -Dserver.ip=192.168.1.15 -jar MainApp.jar
```

### Lưu ý khi không kết nối được

- Các máy phải cùng mạng Wi-Fi/LAN.
- Máy server phải đang chạy `ServerApp.jar`.
- Windows Firewall trên máy server phải cho phép TCP port `8080`.
- Không dùng `127.0.0.1` cho máy client khác, vì `127.0.0.1` chỉ là chính máy đang chạy client.

App cũng đọc `server.ip` và `server.port` từ file `application.properties` đặt cạnh file JAR,
biến môi trường `AUCTION_SERVER_IP` / `AUCTION_SERVER_PORT`, hoặc file chỉ định bằng
`-Dnetwork.config.file=client.properties`.

## Chạy Server Trên Máy Chủ Ảo Azure

Trường hợp muốn chạy `ServerApp.jar` trên máy chủ ảo Azure, máy Azure sẽ đóng vai trò
server public. Các máy client ở bất kỳ mạng nào có Internet sẽ chạy `MainApp.jar` và kết nối
vào public IP của máy Azure.

### 1. Trên Azure Portal

Vào máy ảo `AuctionServer` và kiểm tra:

- VM phải ở trạng thái `Running`. Nếu đang `Stopped (deallocated)` thì bấm `Start`.
- Ghi lại `Public IP address` của VM.
- Vào `Networking` / `Network settings`, thêm inbound rule cho TCP port `8080`.

Ví dụ trong Azure Portal, public IP của máy server là:

```text
4.194.28.97
```

### 2. Trên máy Azure Linux

Kết nối SSH vào VM:

```bash
ssh admin_btl@4.194.28.97
```

Nhập mật khẩu khi terminal hỏi. Không ghi mật khẩu vào README hoặc commit lên Git.

Cài Java 21 nếu máy chưa có:

```bash
sudo apt update
sudo apt install -y openjdk-21-jre
```

Upload `ServerApp.jar` lên máy Azure. Chạy lệnh này từ máy local đang có file JAR:

```bash
scp target/ServerApp.jar admin_btl@4.194.28.97:~/ServerApp.jar
```

Sau đó trên máy Azure chạy server:

```bash
java -jar ServerApp.jar
```

Server sẽ lắng nghe ở port `8080`.

### 3. Trên các máy client

Các máy client chạy `MainApp.jar` và truyền public IP của Azure VM:

```bash
java -jar MainApp.jar --server-ip=4.194.28.97
```

Ví dụ: nếu Azure VM chạy `ServerApp.jar` có public IP là `4.194.28.97`, thì mọi client chạy:

```bash
java -jar MainApp.jar --server-ip=4.194.28.97
```

### Lưu ý khi dùng Azure

- Azure VM phải đang chạy, không được ở trạng thái `Stopped (deallocated)`.
- Azure Network Security Group phải mở inbound TCP port `8080`.
- Nếu trong Linux có bật firewall như `ufw`, cần cho phép port `8080`.
- Public IP của VM có thể thay đổi nếu chưa cấu hình static IP. Nếu IP thay đổi, client phải chạy lại với IP mới.

File `application.properties` đang được đóng gói vào JAR để nhóm có thể chạy server trực tiếp. Nếu cần đổi database khi triển khai, có thể tạo file cấu hình riêng và chạy `java -Ddb.config.file=application-local.properties -jar ServerApp.jar`.

## Cấu Trúc Tổng Quan

```text
src/
├── main/
│   ├── java/com/auction/
│   │   ├── client/      # JavaFX client, controller, socket client, cache UI
│   │   ├── common/      # DTO, enum, model, exception, util dùng chung
│   │   └── server/      # Socket server, handler, manager nghiệp vụ, DAO, DB
│   └── resources/
│       ├── application.properties
│       ├── logback.xml
│       ├── css/
│       ├── fxml/
│       └── images/
└── test/
    ├── java/com/auction/
    └── resources/application-test-h2.properties
```

## Tầng Client

Client là ứng dụng JavaFX. Controller đọc dữ liệu từ FXML, tạo JSON request hoặc DTO, gửi qua `ClientSocket`, rồi cập nhật UI khi nhận response/push event.

```text
client/
├── MainApp.java
├── controller/
│   ├── MainController.java
│   ├── admin/
│   ├── auth/
│   ├── bidder/
│   ├── components/
│   └── seller/
├── interfaces/
├── manager/
├── networkclient/
└── util/
```

### Client Core

| File | Nhiệm vụ |
|---|---|
| `MainApp.java` | Entry point JavaFX, cấu hình stage chính. |
| `MainController.java` | Controller khung chính nếu dùng layout tổng. |
| `networkclient/ClientApplication.java` | Lớp hỗ trợ khởi tạo phía client. |
| `networkclient/ClientSocket.java` | Quản lý kết nối socket tới server, gửi JSON request, map response theo `requestId`/type. |
| `networkclient/PushHandler.java` | Nhận sự kiện server push như bid mới, trạng thái phiên, chat, notification và gọi controller tương ứng. |

### Client Auth

| File | Nhiệm vụ |
|---|---|
| `controller/auth/LoginController.java` | Xử lý đăng nhập, điều hướng theo role `ADMIN`, `BIDDER`, `SELLER`. |
| `controller/auth/RegisterController.java` | Xử lý đăng ký tài khoản bidder/seller. |
| `controller/auth/UserSession.java` | Lưu trạng thái phiên đăng nhập hiện tại: user id, role, tên hiển thị. |

### Client Bidder

| File | Nhiệm vụ |
|---|---|
| `AuctionListScreenController.java` | Danh sách phiên đấu giá, lọc/tìm kiếm, mở phòng đấu giá. |
| `MainDashboardController.java` | Dashboard bidder, thống kê nhanh và điều hướng. |
| `MyAuctionsController.java` | Các phiên bidder đã tham gia. |
| `FollowedAuctionsController.java` | Các phiên bidder đang theo dõi. |
| `AuctionRoomController.java` | Điều phối phòng đấu giá: load snapshot, bid thường, auto-bid, mua đứt, realtime update. |
| `AuctionRoomBase.java` | Lớp nền giữ FXML field, state chung, countdown và helper cơ bản của phòng đấu giá. |
| `AuctionRoomSnapshotSupport.java` | Apply snapshot/refresh state từ server cho phòng đấu giá. |
| `AuctionRoomHistorySupport.java` | Load lịch sử bid, cập nhật chart và realtime bid/status. |
| `AuctionRoomCommandSupport.java` | Command UI đặt giá, mua đứt, đăng ký/xóa auto-bid. |
| `AuctionRoomChartHelper.java` | Parse lịch sử bid và setup điểm chart/tooltip. |
| `AuctionRoomViewHelper.java` | Dialog, alert, countdown label và trạng thái UI hết hạn. |
| `AuctionRoomImageHelper.java` | Căn ảnh sản phẩm theo kiểu cover trong khung cố định. |
| `AuctionRoomImageLoader.java` | Tải ảnh detail/preview từ cache và xử lý fallback khi ảnh lỗi. |
| `AuctionRoomMoneyFormatter.java` | Format/parse tiền VND và formatter cho `TextField`. |
| `AuctionJsonReader.java` | Đọc field từ `JsonObject` an toàn với fallback. |

### Client Seller

| File | Nhiệm vụ |
|---|---|
| `PostAuctionController.java` | Form đăng sản phẩm và tạo phiên đấu giá liên quan. |
| `PostAuctionPreviewBinder.java` | Binding live preview, counter mô tả, preview tiền/thời gian/anti-sniping. |
| `PostAuctionPreviewControls.java` | Record gom các control cần cho preview binder. |
| `PostAuctionFormMapper.java` | Parse tiền, category và thời gian từ form đăng sản phẩm. |
| `MyProductsController.java` | Quản lý sản phẩm/phiên của seller. |
| `MyProductsRenderer.java` | Lọc, dựng card sản phẩm theo batch và gắn action sửa/xóa. |
| `MyProductEditDialog.java` | Dialog sửa sản phẩm seller. |
| `MyProductEditRequest.java` | Record dữ liệu từ dialog sửa sản phẩm. |
| `MyProductsHelper.java` | Helper render/format cho màn hình sản phẩm seller. |

### Client Admin

| File | Nhiệm vụ |
|---|---|
| `AdminLayoutController.java` | Layout admin, sidebar, header, logout, load màn hình con. |
| `DashboardViewController.java` | Dashboard tổng quan admin. |
| `AuctionsViewController.java` | Quản lý phiên đấu giá. |
| `ProductsCensorController.java` | Duyệt hoặc từ chối sản phẩm/phiên chờ duyệt. |
| `BiddersViewController.java` | Quản lý tài khoản bidder. |
| `SellersViewController.java` | Quản lý tài khoản seller. |
| `TransactionsViewController.java` | Quản lý giao dịch. |
| `InvoicesController.java` | Quản lý hóa đơn. |
| `manager/AdminManager.java` | Client-side API wrapper cho các request admin. |
| `manager/AdminResponseMapper.java` | Map JSON response admin sang DTO phía client. |

### Client Components Và Util

| File | Nhiệm vụ |
|---|---|
| `components/SidebarController.java` | Sidebar chung cho bidder/seller, điều hướng, logout, notification badge. |
| `components/ProductCardController.java` | Card hiển thị sản phẩm/phiên đấu giá. |
| `components/ProductCardStatusView.java` | Helper trạng thái badge/action button/countdown của product card. |
| `components/ProductCardImageLoader.java` | Load/preload ảnh card bằng cache. |
| `components/ProductCardSnapshotFactory.java` | Tạo snapshot tối thiểu khi mở phòng từ card. |
| `components/ChatController.java` | Chat/thông báo realtime trong ứng dụng. |
| `components/ChatNotificationRenderer.java` | Render card thông báo thường/thanh toán và countdown thanh toán. |
| `components/ChatTimeUtil.java` | Parse/format thời gian notification. |
| `components/WalletController.java` | UI ví và giao dịch ví. |
| `interfaces/RefreshableCenterContent.java` | Contract cho màn hình có thể refresh khi được load lại. |
| `interfaces/CategoryFilterListener.java` | Contract nhận filter danh mục từ sidebar. |
| `util/AuctionTimeUtil.java` | Parse thời gian và tính trạng thái/countdown phiên đấu giá. |
| `util/AuctionWarmupCache.java` | Cache khởi động trước dữ liệu đấu giá. |
| `util/ClientTaskExecutor.java` | Chạy task nền phía client. |
| `util/CloudStorageUtil.java` | Upload/resolve ảnh qua cloud storage. |
| `util/ImageCacheManager.java` | Cache ảnh preview/detail. |
| `util/ViewCacheManager.java` | Cache FXML/view để giảm load lại UI. |

## Tầng Common

`common` là phần dùng chung cho cả client và server. Không phụ thuộc JavaFX hoặc database.

```text
common/
├── dto/
├── enums/
├── exception/
├── model/
├── observer/
└── util/
```

| Nhóm/File | Nhiệm vụ |
|---|---|
| `dto/BaseDTOs.java` | Request/response gốc, response lỗi chung. |
| `dto/AuthDTOs.java` | DTO đăng nhập, đăng ký, logout. |
| `dto/AuctionDTOs.java` | DTO đấu giá: bid request, auction summary, điểm chart. |
| `dto/ItemDTOs.java` | DTO sản phẩm. |
| `dto/AdminDTOs.java` | DTO màn hình admin: user summary, transaction, invoice, auction summary. |
| `enums/ActionType.java` | Danh sách action string client gửi lên server. Đây là contract protocol chính. |
| `enums/AuctionStatus.java` | Trạng thái phiên đấu giá. |
| `enums/StatusCode.java` | Mã trạng thái response nghiệp vụ. |
| `enums/ErrorCode.java` | Mã lỗi chuẩn hóa. |
| `exception/*` | Exception nghiệp vụ như lỗi xác thực, bid không hợp lệ, phiên đã đóng. |
| `model/bid/*` | Domain model đấu giá: `Auction`, `BidTransaction`, `BidLine`, `AutoBidConfig`. |
| `model/item/*` | Domain model sản phẩm và factory theo loại: art, electronics, vehicle, other. |
| `model/user/*` | Domain model người dùng: `User`, `Admin`, `Bidder`, `Seller`. |
| `model/entity/Entity.java` | Base entity có id. |
| `observer/AuctionObserver.java` | Contract observer để server push sự kiện auction tới client handler. |
| `util/GsonConfig.java` | Gson singleton có adapter cho thời gian. |
| `util/LocalDateTimeAdapter.java` | Serialize/deserialize `LocalDateTime`. |

## Tầng Server

Server nhận kết nối socket, đọc JSON request, dispatch theo `ActionType`, gọi manager nghiệp vụ, manager gọi DAO để đọc/ghi database, sau đó trả response hoặc push realtime event về client.

```text
server/
├── ServerApp.java
├── db/
├── dao/
├── manager/
└── networkserver/
    └── handler/
```

### Server Network

| File | Nhiệm vụ |
|---|---|
| `ServerApp.java` | Entry point server. |
| `networkserver/ServerManager.java` | Lắng nghe port, nhận socket client, tạo `ClientHandler`. |
| `networkserver/ClientHandler.java` | Quản lý một client connection, đọc request, ghi response, giữ `currentUser`, nhận push từ observer. |
| `networkserver/RequestDispatcher.java` | Map `ActionType` sang handler tương ứng. |
| `handler/RequestHandler.java` | Interface chung cho các handler xử lý request. |

### Server Handlers

| File | Nhiệm vụ |
|---|---|
| `handler/AuthController.java` | Login, register, logout, khóa/mở tài khoản. |
| `handler/AuthWalletHandler.java` | Nạp/rút tiền, lịch sử ví và notification biến động số dư. |
| `handler/AuctionController.java` | Dispatcher mỏng cho các `ActionType` thuộc đấu giá. |
| `handler/AuctionCommandHandler.java` | Nhóm command ghi dữ liệu/trạng thái: join/leave, đặt giá, auto-bid, buy-now, settle, close auction. |
| `handler/AuctionQueryHandler.java` | Nhóm request truy vấn auction: list, joined, followed, detail, history, dashboard stats. |
| `handler/AuctionAccountGuard.java` | Kiểm tra seller tự bid và trạng thái khóa/tạm khóa của bidder. |
| `handler/AuctionControllerUtil.java` | Helper response, copy `requestId`, build auction summary, kiểm tra seller của phiên. |
| `handler/AuctionMiscHandler.java` | Các action phụ: follow/unfollow, system notification, chat. |
| `handler/AuctionNotificationService.java` | Tạo payload push realtime cho auction event. |
| `handler/ProductController.java` | Tạo/cập nhật/xóa/lấy sản phẩm và phiên liên quan seller. |
| `handler/ProductQueryHandler.java` | Query sản phẩm: lấy tất cả và tìm kiếm. |
| `handler/ProductResponseMapper.java` | Map auction/item sang JSON response sản phẩm seller. |
| `handler/AdminController.java` | API admin: thống kê, quản lý user, duyệt phiên, giao dịch, hóa đơn. |

### Server Managers

| File | Nhiệm vụ |
|---|---|
| `manager/UserManager.java` | Nghiệp vụ đăng nhập, logout, quản lý session user. |
| `manager/ProductManager.java` | Nghiệp vụ sản phẩm và tạo phiên từ sản phẩm. |
| `manager/AuctionManager.java` | Facade nghiệp vụ đấu giá: đặt giá, mua đứt, auto-bid, quản lý phiên đang chạy. |
| `manager/AuctionLifecycleService.java` | Scheduler mở/đóng phiên, dọn observer và kích hoạt thanh toán sau kết thúc. |
| `manager/AuctionRealtimeNotifier.java` | Observer/realtime push cho bid, status, chat và auction changed. |
| `manager/AuctionBidCommandService.java` | Mua đứt, đăng ký/xóa auto-bid và validation command bid phụ trợ. |
| `manager/AuctionAdminSyncService.java` | Đồng bộ RAM/scheduler sau thao tác admin duyệt, đổi trạng thái hoặc xóa phiên. |
| `manager/AuctionAutoBidService.java` | Thuật toán auto-bid: chọn bot dẫn đầu, tính giá kế tiếp, xử lý mua đứt tự động. |
| `manager/AuctionPaymentTimeoutService.java` | Lên lịch quá hạn thanh toán, tự hủy phiên và áp dụng xử phạt bidder. |
| `manager/AuctionSettlementNotifier.java` | Gửi thông báo sau khi phiên kết thúc/chờ thanh toán. |
| `manager/SystemNotificationManager.java` | Tạo thông báo hệ thống cho user. |

### Server DAO Và Database

| File | Nhiệm vụ |
|---|---|
| `db/ConnectionProvider.java` | Interface cung cấp connection. |
| `db/DatabaseConnection.java` | Cấu hình HikariCP/MySQL, đọc `application.properties`. |
| `db/SetupDatabase.java` | Khởi tạo schema/database khi cần. |
| `dao/UserDao.java` | CRUD user, tìm user theo username/id, cập nhật trạng thái khóa và mốc `lock_until`. |
| `dao/AdminDao.java` | Query phục vụ admin dashboard, user summary, transaction, invoice, pending auction. |
| `dao/AuctionDao.java` | Ghi/cập nhật phiên: tạo phiên, trạng thái/end time, bid transaction và delegate auto-bid/query. |
| `dao/AuctionQueryDao.java` | Query/list/count/search phiên đấu giá. |
| `dao/AutoBidDao.java` | SQL riêng cho cấu hình auto-bid: upsert, xóa, lấy bot theo phiên. |
| `dao/AuctionRowMapper.java` | Map `ResultSet` sang `Auction`. |
| `dao/BidTransactionDao.java` | Lịch sử bid và thống kê bid. |
| `dao/BidderMoneySellerDao.java` | Giao dịch tiền giữa bidder/seller, thanh toán và trừ phí phạt 10% khi hủy/quá hạn. |
| `dao/BidderPenaltyDao.java` | Ghi nhận vi phạm thanh toán, cập nhật khóa tạm/vĩnh viễn cho bidder. |
| `dao/FollowDao.java` | Theo dõi/hủy theo dõi phiên đấu giá. |
| `dao/ItemDao.java` | CRUD sản phẩm. |
| `dao/SystemNotificationDao.java` | Lưu và đọc notification hệ thống. |
| `dao/WalletTransactionDao.java` | Giao dịch ví. |

## Resources

| Thư mục/File | Nhiệm vụ |
|---|---|
| `resources/fxml/auth` | Màn hình login/register. |
| `resources/fxml/bidder` | Layout và màn hình bidder. |
| `resources/fxml/seller` | Màn hình seller. |
| `resources/fxml/admin` | Layout và màn hình admin. |
| `resources/fxml/components` | Component dùng chung như sidebar, chat, wallet, product card. |
| `resources/css/style.css` | Style chung/bidder/component. |
| `resources/css/seller.css` | Style seller. |
| `resources/css/admin.css` | Style admin. |
| `resources/css/chart.css` | Style chart. |
| `resources/application.properties` | Cấu hình DB/server. |
| `resources/logback.xml` | Cấu hình logging. |

## Luồng Giao Tiếp Client-Server-Database

### 1. Request Đồng Bộ

```text
JavaFX Controller
  -> tạo DTO hoặc JsonObject có type = ActionType.*
  -> ClientSocket.sendJsonRequest(...)
  -> ServerManager/ClientHandler nhận JSON
  -> RequestDispatcher chọn RequestHandler
  -> Handler validate request/currentUser
  -> Manager xử lý nghiệp vụ
  -> DAO đọc/ghi database qua DatabaseConnection
  -> Handler build JSON response
  -> ClientSocket nhận response
  -> callback Platform.runLater(...) cập nhật UI
```

Ví dụ đặt giá:

```text
AuctionRoomController.handlePlaceBid()
  -> ActionType.PLACE_BID
  -> AuctionController.handlePlaceBid()
  -> AuctionManager xử lý bid
  -> AuctionDao/BidTransactionDao ghi bid và cập nhật phiên
  -> response BID_RESPONSE
  -> client refreshAuctionState() + loadBidHistoryFromServer()
```

### 2. Push Realtime

```text
Client join auction
  -> ActionType.JOIN_AUCTION
  -> AuctionManager.subscribe(auctionId, ClientHandler)

Khi có bid/status/chat mới:
  -> AuctionManager notify observer
  -> ClientHandler gửi JSON push qua socket
  -> PushHandler nhận push
  -> gọi controller đang mở phòng đấu giá/chat/admin
  -> UI cập nhật realtime
```

### 3. Database

Server là nơi duy nhất truy cập database. Client không gọi database trực tiếp.

```text
Client UI
  -> Socket JSON
  -> Server Handler
  -> Manager nghiệp vụ
  -> DAO
  -> MySQL/H2
```

## Quy Tắc Thanh Toán Và Khóa Tài Khoản

- Khi bidder thắng phiên nhưng hủy hoặc quá hạn thanh toán, hệ thống kiểm tra ví để xử lý phí phạt 10% giá chốt.
- Nếu ví đủ tiền, hệ thống tự trừ 10%, hủy phiên và ghi biến động số dư; bidder không bị khóa tài khoản.
- Nếu ví không đủ tiền, hệ thống hủy phiên và ghi nhận vi phạm thanh toán.
- Vi phạm lần 1 khóa tài khoản 3 ngày, lần 2 khóa 7 ngày, từ lần 3 khóa vĩnh viễn.
- Trạng thái khóa được lưu trực tiếp trong bảng `users.status`. Khóa tạm thời lưu thêm mốc mở lại ở `users.lock_until` để admin xem được.
- Khi bidder đăng nhập trong thời gian khóa tạm, server trả thông báo còn bao lâu và thời điểm được đăng nhập lại. Nếu `lock_until` đã hết hạn, login sẽ tự mở lại tài khoản.
- Khóa vĩnh viễn hoặc khóa thủ công bởi admin có `users.status = LOCKED` và `users.lock_until = NULL`; khi login sẽ báo tài khoản bị khóa vĩnh viễn và cần liên hệ Admin.

## Kiểm Thử

- Unit test và integration test nằm trong `src/test/java`.
- Test DAO dùng H2 qua `src/test/resources/application-test-h2.properties`.
- `mvn test` hiện chạy cả checkstyle, compile và test.

### Client Tests

| File test | Phần được test | Nội dung kiểm tra chính |
|---|---|---|
| `BiddingLogicTest.java` | Logic đặt giá phía client | Giá bid hợp lệ phải lớn hơn giá hiện tại, giá thấp hơn bị xem là không hợp lệ. |
| `StatusMappingTest.java` | Mapping thông báo lỗi/trạng thái client | Message hiển thị cho lỗi đăng nhập và phiên đấu giá đã kết thúc. |
| `UserSessionTest.java` | Session đăng nhập phía client | Set/get thông tin user hiện tại và `clear()` reset toàn bộ session. |
| `AuctionTimeUtilTest.java` | Tiện ích thời gian đấu giá | Parse thời gian ISO và tính countdown theo server clock cho trạng thái chưa mở/đang chạy. |
| `AdminResponseMapperTest.java` | Mapping response admin phía client | Map danh sách user với status mặc định, map pending auctions với fallback an toàn, map transactions cho báo cáo admin. |

### Common Tests

| File test | Phần được test | Nội dung kiểm tra chính |
|---|---|---|
| `AuctionTest.java` | Domain model `Auction` | Cập nhật winner/giá/lịch sử bid, tính trạng thái theo thời gian, giữ trạng thái terminal và extend end time. |
| `AutoBidConfigTest.java` | Domain model auto-bid | Thứ tự ưu tiên auto-bid theo max bid, bid step và thời điểm đăng ký. |
| `ItemFactoryTest.java` | Factory tạo sản phẩm | Tạo đúng subtype art/electronics/vehicle/other, map đủ field và trả `null` khi input không hợp lệ. |
| `UserTest.java` | Domain model user | Role name, thao tác balance và trạng thái mặc định/chuyển trạng thái user. |
| `GsonConfigTest.java` | Gson dùng chung | Singleton Gson, serialize/deserialize `LocalDateTime`, lỗi parse ngày không hợp lệ. |

### Server DAO/Integration Tests

| File test | Phần được test | Nội dung kiểm tra chính |
|---|---|---|
| `UserDaoIntegrationTest.java` | `UserDao` với H2 | Lưu user, tìm theo username và cập nhật balance trong database test. |
| `ItemDaoIntegrationTest.java` | `ItemDao` với H2 | Lưu, đọc, sửa, tìm kiếm theo keyword/seller và xóa sản phẩm. |
| `AuctionDaoIntegrationTest.java` | `AuctionDao` với H2 | Tạo seller/bidder/item/auction rồi đặt bid để kiểm tra cập nhật giá hiện tại và winner trong DB. |
| `AutoBidDaoIntegrationTest.java` | `AutoBidDao` với H2 | Lưu/cập nhật auto-bid, đọc max bid/bid step, lấy danh sách bot theo phiên và xóa auto-bid. |
| `FollowAndBidHistoryDaoIntegrationTest.java` | `FollowDao` và `BidTransactionDao` với H2 | Follow/unfollow/count phiên theo dõi, lưu và đọc lịch sử bid của một phiên. |
| `AdminDaoIntegrationTest.java` | `AdminDao` với H2 | Lọc pending auctions, chỉ lấy hóa đơn `PAID`, chỉ lấy giao dịch terminal `FINISHED/PAID/CANCELED`. |
| `AdminDaoWriteActionsIntegrationTest.java` | `AdminDao` write-actions với H2 | Duyệt phiên, đổi trạng thái, đọc thông tin phiên và xóa phiên đấu giá. |
| `BidderMoneySellerDaoIntegrationTest.java` | Quyết toán tiền bidder-seller với H2 | Bidder thắng thanh toán thành công, cập nhật balance, wallet history, status `PAID`; xử lý phí phạt 10% khi hủy/quá hạn; chặn người không thắng thanh toán. |
| `UserLockIntegrationTest.java` | Khóa tài khoản với H2 | Vi phạm thanh toán ghi `users.status/lock_until`, login báo khóa tạm kèm mốc mở lại, khóa vĩnh viễn báo liên hệ Admin và khóa tạm hết hạn tự mở. |
| `AdminControllerReadActionsTest.java` | Read-actions admin qua controller + H2 | Response cho pending auctions, invoices, transactions có đúng `type`, `success`, `requestId` và data liên quan. |

### Server Manager Tests

| File test | Phần được test | Nội dung kiểm tra chính |
|---|---|---|
| `ProductManagerTest.java` | `ProductManager` | Tạo đúng subtype sản phẩm theo category và trả `null` khi dữ liệu đầu vào không hợp lệ. |
| `AuctionAutoBidServiceTest.java` | `AuctionAutoBidService` | Nhiều auto-bid cạnh tranh, auto-bid thắng theo max bid; nhiều manual-bid xen kẽ auto-bid cho tới khi vượt max bid. |

### Server Network/Handler Tests

| File test | Phần được test | Nội dung kiểm tra chính |
|---|---|---|
| `RequestDispatcherTest.java` | `RequestDispatcher` | Action không tồn tại trả lỗi chuẩn, controller trả `null` được bọc thành lỗi fallback và giữ `requestId`. |
| `AuthControllerTest.java` | `AuthController` | Request `null`, thiếu `type`, action không hỗ trợ đều trả lỗi bad request. |
| `AuctionControllerTest.java` | `AuctionController` | Validate request đấu giá lỗi: request null, action không hỗ trợ, tạo auction trực tiếp, join/leave thiếu hoặc sai auction id. |
| `ProductControllerTest.java` | `ProductController` | Validate request sản phẩm lỗi: request null, action không hỗ trợ, search thiếu keyword, delete/get thiếu item id hợp lệ. |
| `AdminControllerTest.java` | `AdminController` với H2 | Kiểm tra payload admin cho pending auctions, tổng doanh thu invoices và transactions terminal. |

### Test Suite Và Helper

| File | Vai trò |
|---|---|
| `AllTestSuite.java` | Gom nhóm chạy test suite JUnit. |
| `DaoIntegrationTestSupport.java` | Khởi tạo H2 schema dùng chung cho integration test DAO/controller. |
| `AdminTestData.java` | Helper tạo và cleanup seller, bidder, item, auction cho test admin. |
