# Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)

Ứng dụng đấu giá theo mô hình client-server, viết bằng Java 21 + JavaFX, giao tiếp socket JSON.

## Tổng Quan Kiến Trúc

- `client`: JavaFX UI, FXML, controller theo role (`admin`, `bidder`, `seller`), kết nối server qua `ClientSocket`.
- `server`: nhận request socket, dispatch theo `ActionType`, xử lý nghiệp vụ qua `manager`, truy cập DB qua `dao`.
- `common`: DTO, enum, model, exception, util dùng chung cho cả client và server.
- `test`: unit test và integration test (H2 in-memory cho test).

## Công Nghệ

- Java 21
- JavaFX 21
- Maven
- Gson
- MySQL Connector/J
- HikariCP
- H2 (test)
- JUnit 5 + AssertJ
- SLF4J + Logback

## Entry Points

- Client: `com.auction.client.MainApp`
- Server: `com.auction.server.ServerApp`

`pom.xml` hiện đã cấu hình đúng:
- JavaFX plugin chạy `com.auction.client.MainApp`
- Shade plugin đóng gói server với `com.auction.server.ServerApp`

## Cấu Trúc Thư Mục

```text
src/
├── main/
│   ├── java/com/auction/
│   │   ├── client/
│   │   │   ├── controller/
│   │   │   │   ├── admin/
│   │   │   │   ├── auth/
│   │   │   │   ├── bidder/
│   │   │   │   ├── components/
│   │   │   │   └── seller/
│   │   │   ├── interfaces/
│   │   │   ├── manager/
│   │   │   ├── networkclient/
│   │   │   └── util/
│   │   ├── common/
│   │   │   ├── dto/
│   │   │   ├── enums/
│   │   │   ├── exception/
│   │   │   ├── model/
│   │   │   ├── observer/
│   │   │   └── util/
│   │   └── server/
│   │       ├── dao/
│   │       ├── db/
│   │       ├── manager/
│   │       └── networkserver/
│   │           └── handler/
│   └── resources/
│       ├── application.properties
│       ├── logback.xml
│       ├── css/
│       ├── fxml/
│       └── images/
└── test/
    ├── java/com/auction/...
    └── resources/
```

## Chạy Dự Án

Chạy test:

```bash
mvn test
```

Chạy client:

```bash
mvn javafx:run
```

Chạy server (IDE): chạy class `com.auction.server.ServerApp`.

## Ghi Chú

- Nếu đọc file tiếng Việt trong terminal Windows bị lỗi dấu, kiểm tra lại encoding terminal (`UTF-8`) thay vì nội dung file.
