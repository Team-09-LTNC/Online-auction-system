# Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)

README này mô tả cấu trúc hiện tại của dự án và nhiệm vụ của từng file để phục vụ đọc hiểu, bảo trì và kiểm thử.

## Tổng Quan

Dự án là ứng dụng đấu giá trực tuyến theo mô hình client-server:

- Client: JavaFX, FXML, controller theo màn hình, giao tiếp server bằng socket JSON.
- Server: socket đa client, dispatcher theo `ActionType`, controller xử lý request, manager xử lý nghiệp vụ, DAO truy cập database.
- Common: DTO, enum, model, exception và tiện ích dùng chung giữa client/server.
- Test: JUnit 5, H2 in-memory database cho integration test.

## Công Nghệ Chính

- Java 21
- JavaFX 21
- Maven
- Gson
- MySQL Connector/J
- HikariCP
- H2 Database cho test
- JUnit 5, AssertJ
- SLF4J + Logback

## Điểm Chạy Chính

| File | Nhiệm vụ |
|---|---|
| `src/main/java/com/auction/client/MainApp.java` | Entry point phía client, set encoding UTF-8 rồi gọi `ClientApplication`. |
| `src/main/java/com/auction/client/networkclient/ClientApplication.java` | JavaFX `Application`, nạp màn đăng nhập và khởi tạo cửa sổ client. |
| `src/main/java/com/auction/server/ServerApp.java` | Entry point phía server, đọc cấu hình và khởi động `ServerManager`. |
| `pom.xml` | Cấu hình Maven, dependency, compiler Java 21, surefire test, JavaFX plugin, shade plugin, checkstyle. |

Lưu ý: trong `pom.xml` hiện có một số tên main class cũ trong plugin (`com.auction.client.networkclient.MainApp`, `com.auction.server.Server`) không trùng với file entry point hiện tại. Khi chạy bằng IDE, hãy dùng `com.auction.client.MainApp` và `com.auction.server.Sever`.

## Cấu Trúc Thư Mục

```text
src/
├── main/
│   ├── java/com/auction/
│   │   ├── client/                 # JavaFX client, controller, socket client, cache UI
│   │   ├── common/                 # DTO, enum, model, exception, Gson config dùng chung
│   │   └── server/                 # Server socket, DAO, manager, request handler
│   └── resources/
│       ├── css/                    # Style JavaFX
│       ├── fxml/                   # Layout JavaFX theo role/màn hình
│       ├── images/                 # Ảnh tĩnh trong app
│       ├── application.properties  # Cấu hình database và socket runtime
│       └── logback.xml             # Cấu hình log
└── test/
    ├── java/com/auction/           # Unit/integration tests
    └── resources/                  # Cấu hình H2 cho test
```

## Luồng Xử Lý Chính

1. Client mở `Login.fxml` qua `ClientApplication`.
2. `LoginController` gửi request đăng nhập bằng `ClientSocket`.
3. Server nhận socket trong `ClientHandler`, chuyển JSON cho `RequestDispatcher`.
4. `RequestDispatcher` chọn handler theo `ActionType`.
5. Handler gọi manager/DAO, trả JSON response về client.
6. Client listener trong `ClientSocket` map response theo `requestId` và gọi callback.
7. Push realtime như bid update, kết quả phiên, notification được chuyển sang `PushHandler`.

## Client Code

### `com.auction.client`

| File | Nhiệm vụ |
|---|---|
| `MainApp.java` | Entry point client, ép encoding UTF-8 và gọi `ClientApplication.main`. |

### `com.auction.client.networkclient`

| File | Nhiệm vụ |
|---|---|
| `ClientApplication.java` | Khởi tạo JavaFX stage, nạp màn login đầu tiên. |
| `ClientSocket.java` | Singleton quản lý socket client, gửi JSON request, tự sinh `requestId`, lưu callback response, đọc response/push từ server bằng listener thread. |
| `PushHandler.java` | Xử lý event server đẩy chủ động: cập nhật giá realtime, kết quả phiên, notification hệ thống. |

### `com.auction.client.controller`

| File | Nhiệm vụ |
|---|---|
| `MainController.java` | Quản lý vùng nội dung chính của layout bidder/seller, cache một số view, chuyển màn center content. |

### `com.auction.client.controller.auth`

| File | Nhiệm vụ |
|---|---|
| `LoginController.java` | Xử lý đăng nhập, mở socket sớm, lưu `UserSession`, chuyển sang layout theo role, warm-up cache auction sau login. |
| `RegisterController.java` | Xử lý đăng ký tài khoản và chuyển màn auth. |
| `UserSession.java` | Lưu session client hiện tại: userId, username, role; hỗ trợ clear khi logout. |

### `com.auction.client.controller.admin`

| File | Nhiệm vụ |
|---|---|
| `AdminLayoutController.java` | Layout chính admin, điều hướng các màn dashboard, auctions, users, transactions, invoices. |
| `DashboardViewController.java` | Hiển thị thống kê admin tổng quan. |
| `AuctionsViewController.java` | Hiển thị/quản lý bảng phiên đấu giá admin, lọc và đóng phiên. |
| `ProductsCensorController.java` | Màn duyệt sản phẩm/phiên chờ duyệt. |
| `BiddersViewController.java` | Quản lý danh sách bidder, trạng thái và thao tác admin liên quan bidder. |
| `SellersViewController.java` | Quản lý danh sách seller. |
| `TransactionsViewController.java` | Hiển thị giao dịch trong hệ thống. |
| `InvoicesController.java` | Hiển thị hóa đơn/thanh toán. |

### `com.auction.client.controller.bidder`

| File | Nhiệm vụ |
|---|---|
| `MainDashboardController.java` | Dashboard bidder, tải phiên nổi bật, thống kê nhanh, dùng warm-up cache nếu có. |
| `AuctionListScreenController.java` | Màn tất cả phiên đấu giá, lọc theo category/status/search, render card theo batch, dùng cache danh sách nếu có. |
| `AuctionRoomController.java` | Phòng đấu giá: join phiên, hiển thị thông tin sản phẩm, countdown, đặt giá, auto-bid, mua đứt, nhận update realtime, tải lịch sử bid. |
| `AuctionRoomChartHelper.java` | Helper cho biểu đồ giá đấu trong phòng, setup hover/tooltip cho điểm dữ liệu. |
| `AuctionRoomViewHelper.java` | Helper UI cho phòng đấu giá, tách bớt thao tác view khỏi controller chính. |
| `MyAuctionsController.java` | Màn phiên mà bidder đã tham gia, lọc và render danh sách tương tự auction list. |
| `FollowedAuctionsController.java` | Màn phiên bidder đang theo dõi, tải danh sách follow và render card. |

### `com.auction.client.controller.components`

| File | Nhiệm vụ |
|---|---|
| `SidebarController.java` | Điều hướng sidebar, áp dụng phân quyền theo role, badge notification, logout và clear cache. |
| `ProductCardController.java` | Controller card sản phẩm/phiên, hiển thị ảnh/giá/countdown/trạng thái, follow/unfollow, vào phòng đấu giá, thao tác seller edit/delete. |
| `ChatController.java` | Màn thông báo/chat hệ thống, nhận notification từ `PushHandler`. |
| `WalletController.java` | Màn ví/giao dịch tiền của người dùng. |

### `com.auction.client.controller.seller`

| File | Nhiệm vụ |
|---|---|
| `PostAuctionController.java` | Màn seller đăng sản phẩm/phiên mới, nhập thông tin sản phẩm, thời gian, giá, upload ảnh. |
| `MyProductsController.java` | Màn seller quản lý sản phẩm/phiên của mình, lọc, thống kê, sửa/xóa phiên còn OPEN. |
| `MyProductsHelper.java` | Helper parse/format thời gian, trạng thái, lấy field JSON an toàn cho `MyProductsController`. |

### `com.auction.client.interfaces`

| File | Nhiệm vụ |
|---|---|
| `CategoryFilterListener.java` | Interface để sidebar truyền category vào màn có hỗ trợ lọc. |
| `RefreshableCenterContent.java` | Interface cho view center có thể refresh khi được mở lại từ cache. |

### `com.auction.client.manager`

| File | Nhiệm vụ |
|---|---|
| `AdminManager.java` | Client-side manager gọi request admin và map response sang model hiển thị admin. |

### `com.auction.client.util`

| File | Nhiệm vụ |
|---|---|
| `AuctionTimeUtil.java` | Parse thời gian server, tính trạng thái hiển thị OPEN/RUNNING/FINISHED và countdown. |
| `AuctionWarmupCache.java` | Cache response danh sách/featured auctions sau login, preload ảnh để giảm cảm giác chờ khi mở dashboard/list/phòng. |
| `ImageCacheManager.java` | Cache ảnh sản phẩm trong RAM, preload ảnh preview bằng thread nền. |
| `ViewCacheManager.java` | Cache `Parent` FXML đã load để chuyển màn nhanh hơn. |
| `CloudStorageUtil.java` | Tiện ích upload/lưu ảnh lên cloud storage hoặc xử lý URL ảnh sản phẩm. |

## Common Code

### `com.auction.common.dto`

| File | Nhiệm vụ |
|---|---|
| `BaseDTOs.java` | DTO nền cho request/response/error dùng chung. |
| `AuthDTOs.java` | DTO đăng nhập, đăng ký và phản hồi xác thực. |
| `AuctionDTOs.java` | DTO danh sách phiên, bid request, update giá, lịch sử giá. |
| `ItemDTOs.java` | DTO sản phẩm, tạo/cập nhật sản phẩm và dữ liệu liên quan item. |
| `AdminDTOs.java` | DTO phục vụ màn admin như auction summary, user/transaction/invoice data. |

### `com.auction.common.enums`

| File | Nhiệm vụ |
|---|---|
| `ActionType.java` | Hằng tên action/request/response/push event giữa client và server. |
| `AuctionStatus.java` | Enum trạng thái phiên: OPEN, RUNNING, FINISHED, PAID, CANCELED... |
| `ErrorCode.java` | Hằng mã lỗi nghiệp vụ/API. |
| `StatusCode.java` | Hằng mã trạng thái response tương tự HTTP-style. |

### `com.auction.common.exception`

| File | Nhiệm vụ |
|---|---|
| `AuthenticationException.java` | Exception cho lỗi xác thực. |
| `InvalidBidException.java` | Exception khi giá bid không hợp lệ. |
| `AuctionClosedException.java` | Exception khi thao tác trên phiên đã đóng/kết thúc. |

### `com.auction.common.model.entity`

| File | Nhiệm vụ |
|---|---|
| `Entity.java` | Base model có `id`, dùng chung cho user, item, auction. |

### `com.auction.common.model.user`

| File | Nhiệm vụ |
|---|---|
| `User.java` | Lớp cha người dùng, chứa username/password/fullName/role/status. |
| `Bidder.java` | Model người mua/người đấu giá. |
| `Seller.java` | Model người bán. |
| `Admin.java` | Model quản trị viên. |

### `com.auction.common.model.item`

| File | Nhiệm vụ |
|---|---|
| `Item.java` | Lớp cha sản phẩm, chứa name, description, startingPrice, bidIncrement, sellerId, category, imageUrl. |
| `Electronics.java` | Sản phẩm điện tử, mở rộng `Item`. |
| `Vehicle.java` | Sản phẩm xe cộ, mở rộng `Item`. |
| `Art.java` | Sản phẩm nghệ thuật, mở rộng `Item`. |
| `OtherItem.java` | Sản phẩm loại khác. |
| `ItemAttributes.java` | Đóng gói thuộc tính mở rộng khi tạo item. |
| `ItemFactory.java` | Interface/base factory tạo item theo category. |
| `ElectronicsFactory.java` | Factory tạo `Electronics`. |
| `VehicleFactory.java` | Factory tạo `Vehicle`. |
| `ArtFactory.java` | Factory tạo `Art`. |
| `OtherItemFactory.java` | Factory tạo `OtherItem`. |

### `com.auction.common.model.bid`

| File | Nhiệm vụ |
|---|---|
| `Auction.java` | Model phiên đấu giá, giữ item, thời gian, status, currentHighestBid, winner, buy-now, anti-sniping, bid history. |
| `BidTransaction.java` | Model một lượt đặt giá, gồm auctionId, bidder, amount, bidTime. |
| `BidLine.java` | Dòng lịch sử bid trả về UI/biểu đồ. |
| `AutoBidConfig.java` | Cấu hình auto-bid của bidder trong một phiên. |

### `com.auction.common.observer`

| File | Nhiệm vụ |
|---|---|
| `AuctionObserver.java` | Interface observer để server thông báo bid/status/notification realtime cho client handler. |

### `com.auction.common.util`

| File | Nhiệm vụ |
|---|---|
| `GsonConfig.java` | Cấu hình Gson singleton, đăng ký adapter thời gian và model đa hình nếu cần. |
| `LocalDateTimeAdapter.java` | Adapter serialize/deserialize `LocalDateTime` cho Gson. |

## Server Code

### `com.auction.server`

| File | Nhiệm vụ |
|---|---|
| `ServerApp.java` | Entry point server, đọc config, setup database, mở server socket. |

### `com.auction.server.networkserver`

| File | Nhiệm vụ |
|---|---|
| `ServerManager.java` | Quản lý `ServerSocket`, accept client mới, tạo `ClientHandler` theo kết nối. |
| `ClientHandler.java` | Xử lý một client: đọc JSON, giữ user hiện tại, gửi response/push, implement observer realtime. |
| `RequestDispatcher.java` | Map `ActionType` sang handler tương ứng: auth, auction, product, admin. |

### `com.auction.server.networkserver.handler`

| File | Nhiệm vụ |
|---|---|
| `RequestHandler.java` | Interface chung cho các controller xử lý request server. |
| `AuthController.java` | Xử lý login/register/logout hoặc request xác thực liên quan user. |
| `AuctionController.java` | Xử lý join room, place bid, buy-now, auto-bid, lấy danh sách phiên, lịch sử bid, follow, chat, notification. |
| `AuctionControllerUtil.java` | Helper cho `AuctionController`: lấy auctionId, tạo response lỗi, summary DTO, followedIds. |
| `AuctionMiscHandler.java` | Tách các action phụ của auction như follow/unfollow, notification, chat. |
| `AuctionNotificationService.java` | Gửi notification nghiệp vụ khi mua đứt, kết thúc, quyết toán. |
| `ProductController.java` | Xử lý tạo/cập nhật/xóa/duyệt sản phẩm và request sản phẩm seller/admin. |
| `AdminController.java` | Xử lý request admin: dashboard, danh sách users, auctions, transactions, invoices. |

### `com.auction.server.manager`

| File | Nhiệm vụ |
|---|---|
| `AuctionManager.java` | Nghiệp vụ lõi đấu giá: quản lý phiên đang chạy, synchronized khi đặt giá, anti-sniping, auto-bid, observer. |
| `ProductManager.java` | Nghiệp vụ sản phẩm: tạo item/auction, cập nhật, xóa, duyệt sản phẩm. |
| `UserManager.java` | Nghiệp vụ user: đăng nhập, đăng ký, quản lý trạng thái/tài khoản. |
| `SystemNotificationManager.java` | Tạo và quản lý thông báo hệ thống. |
| `AuctionSettlementNotifier.java` | Theo dõi/thông báo quyết toán sau khi phiên kết thúc hoặc mua đứt. |

### `com.auction.server.dao`

| File | Nhiệm vụ |
|---|---|
| `AdminDao.java` | Query dữ liệu admin: thống kê, users, transactions, invoices. |
| `AuctionDao.java` | Query/cập nhật phiên đấu giá, trạng thái theo thời gian, giá hiện tại, auto-bid, danh sách phiên. |
| `AuctionRowMapper.java` | Map `ResultSet` SQL sang model `Auction` và `Item`. |
| `BidTransactionDao.java` | Lưu/lấy lịch sử bid, thao tác transaction liên quan bid. |
| `BidderMoneySellerDao.java` | Xử lý quyết toán tiền giữa bidder và seller khi mua đứt/thanh toán/hủy. |
| `BidderPenaltyDao.java` | Quản lý trạng thái phạt/tạm khóa bidder khi vi phạm thanh toán. |
| `FollowDao.java` | Lưu/lấy danh sách phiên user theo dõi. |
| `ItemDao.java` | CRUD item/sản phẩm, query sản phẩm theo seller/admin. |
| `SystemNotificationDao.java` | CRUD notification hệ thống, unread count, mark read. |
| `UserDao.java` | CRUD/query user, đăng nhập, trạng thái tài khoản. |
| `WalletTransactionDao.java` | Query/lưu giao dịch ví. |

### `com.auction.server.db`

| File | Nhiệm vụ |
|---|---|
| `ConnectionProvider.java` | Interface/abstraction cung cấp connection, hỗ trợ test và production. |
| `DatabaseConnection.java` | Singleton HikariCP connection pool, đọc config DB từ properties. |
| `SetupDatabase.java` | Khởi tạo schema/bảng/migration dữ liệu cần thiết khi server chạy. |

## Resource Files

### Cấu hình

| File | Nhiệm vụ |
|---|---|
| `src/main/resources/application.properties` | Cấu hình database production/runtime và `server.ip`, `server.port`. |
| `src/main/resources/logback.xml` | Cấu hình log console/file và level logger. |
| `src/test/resources/application-test-h2.properties` | Cấu hình H2 in-memory cho test. |

### CSS

| File | Nhiệm vụ |
|---|---|
| `css/style.css` | Style chung client/bidder. |
| `css/admin.css` | Style cho màn admin. |
| `css/seller.css` | Style cho màn seller. |
| `css/chart.css` | Style biểu đồ giá đấu trong phòng. |

### FXML Auth

| File | Controller | Nhiệm vụ |
|---|---|---|
| `fxml/auth/Login.fxml` | `LoginController` | Giao diện đăng nhập. |
| `fxml/auth/Register.fxml` | `RegisterController` | Giao diện đăng ký. |

### FXML Layout Và Components

| File | Controller | Nhiệm vụ |
|---|---|---|
| `fxml/bidder/MainLayout.fxml` | `MainController` | Layout chính có sidebar và vùng nội dung. |
| `fxml/components/Sidebar.fxml` | `SidebarController` | Thanh điều hướng theo role. |
| `fxml/components/ProductCard.fxml` | `ProductCardController` | Card sản phẩm/phiên dùng ở nhiều màn. |
| `fxml/components/Chat.fxml` | `ChatController` | Màn thông báo/chat. |
| `fxml/components/Wallet.fxml` | `WalletController` | Màn ví. |

### FXML Bidder

| File | Controller | Nhiệm vụ |
|---|---|---|
| `fxml/bidder/MainDashboard.fxml` | `MainDashboardController` | Dashboard bidder. |
| `fxml/bidder/AuctionListScreen.fxml` | `AuctionListScreenController` | Danh sách tất cả phiên. |
| `fxml/bidder/AuctionRoom.fxml` | `AuctionRoomController` | Phòng đấu giá realtime. |
| `fxml/bidder/MyAuctions.fxml` | `MyAuctionsController` | Phiên đã tham gia. |
| `fxml/bidder/FollowedAuctions.fxml` | `FollowedAuctionsController` | Phiên đang theo dõi. |

### FXML Seller

| File | Controller | Nhiệm vụ |
|---|---|---|
| `fxml/seller/SellerDashboard.fxml` | `PostAuctionController` | Đăng sản phẩm/phiên đấu giá. |
| `fxml/seller/MyProducts.fxml` | `MyProductsController` | Quản lý sản phẩm seller. |

### FXML Admin

| File | Controller | Nhiệm vụ |
|---|---|---|
| `fxml/admin/AdminLayout.fxml` | `AdminLayoutController` | Layout chính admin. |
| `fxml/admin/DashboardView.fxml` | `DashboardViewController` | Dashboard admin. |
| `fxml/admin/AuctionsView.fxml` | `AuctionsViewController` | Quản lý phiên. |
| `fxml/admin/ProductsCensorView.fxml` | `ProductsCensorController` | Duyệt sản phẩm. |
| `fxml/admin/BiddersView.fxml` | `BiddersViewController` | Quản lý bidder. |
| `fxml/admin/SellersView.fxml` | `SellersViewController` | Quản lý seller. |
| `fxml/admin/TransactionsView.fxml` | `TransactionsViewController` | Giao dịch. |
| `fxml/admin/InvoicesView.fxml` | `InvoicesController` | Hóa đơn. |

### Images

| File | Nhiệm vụ |
|---|---|
| `images/bua_dau_gia.png` | Ảnh tĩnh dùng trong giao diện/branding. |

## Test Code

| File | Nhiệm vụ |
|---|---|
| `AllTestSuite.java` | Suite JUnit chạy toàn bộ package `com.auction`. |
| `client/BiddingLogicTest.java` | Test logic đặt giá phía client/nghiệp vụ cơ bản. |
| `client/StatusMappingTest.java` | Test mapping trạng thái hiển thị. |
| `client/controller/auth/UserSessionTest.java` | Test lưu/clear session client. |
| `client/util/AuctionTimeUtilTest.java` | Test parse thời gian, trạng thái, countdown. |
| `common/model/bid/AuctionTest.java` | Test model `Auction`: trạng thái, winner, accept bid, extend time. |
| `common/model/bid/AutoBidConfigTest.java` | Test cấu hình auto-bid. |
| `common/model/item/ItemFactoryTest.java` | Test factory tạo item theo category. |
| `common/model/user/UserTest.java` | Test model user và role/status cơ bản. |
| `common/util/GsonConfigTest.java` | Test Gson config, serialize/deserialize model/thời gian. |
| `server/dao/DaoIntegrationTestSupport.java` | Base support cho integration test DAO với H2. |
| `server/dao/UserDaoIntegrationTest.java` | Test tích hợp `UserDao`. |
| `server/dao/AuctionDaoIntegrationTest.java` | Test tích hợp `AuctionDao`. |
| `server/manager/ProductManagerTest.java` | Test nghiệp vụ tạo/quản lý sản phẩm. |
| `server/networkserver/RequestDispatcherTest.java` | Test dispatcher route action và lỗi action không tồn tại. |
| `server/networkserver/handler/AuthControllerTest.java` | Test request/response auth controller. |
| `server/networkserver/handler/AuctionControllerTest.java` | Test request/response auction controller. |
| `server/networkserver/handler/ProductControllerTest.java` | Test request/response product controller. |

## Chạy Dự Án

### Chạy test

```bash
mvn test
```

Test dùng `src/test/resources/application-test-h2.properties` qua surefire system property `db.config.file`.

### Chạy server trong IDE

Chạy class:

```text
com.auction.server.ServerApp
```

Server đọc `src/main/resources/application.properties`, khởi tạo database và mở socket theo `server.port`.

### Chạy client trong IDE

Chạy class:

```text
com.auction.client.MainApp
```

Client đọc `server.ip` và `server.port` trong `application.properties`, sau đó kết nối socket đến server.

