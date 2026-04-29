package com.auction.server.utils;

import java.sql.Connection;
import java.sql.Statement;

public class SetupDatabase {
    public static void main(String[] args) {
        System.out.println(">>> Đang khởi tạo cấu trúc cơ sở dữ liệu...");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Tạo bảng người dùng
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "  id         VARCHAR(36)  PRIMARY KEY, " +
                            "  username   VARCHAR(50)  UNIQUE NOT NULL, " +
                            "  password   VARCHAR(255) NOT NULL, " +
                            "  full_name  VARCHAR(100), " +
                            "  role       ENUM('BIDDER','SELLER','ADMIN') NOT NULL" +
                            ")"
            );

            // Tạo bảng sản phẩm
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS items (" +
                            "  id             INT AUTO_INCREMENT PRIMARY KEY, " +
                            "  seller_id      VARCHAR(36), " +
                            "  name           VARCHAR(100) NOT NULL, " +
                            "  description    TEXT, " +
                            "  category       ENUM('ELECTRONICS','ART','VEHICLE') NOT NULL, " +
                            "  starting_price DOUBLE NOT NULL, " +
                            "  current_price  DOUBLE DEFAULT 0, " +
                            "  status         VARCHAR(20) DEFAULT 'OPEN', " +
                            "  FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE SET NULL" +
                            ")"
            );

            // Tạo bảng phiên đấu giá
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS auctions (" +
                            "  id           INT AUTO_INCREMENT PRIMARY KEY, " +
                            "  item_id      INT NOT NULL, " +
                            "  start_time   DATETIME, " +
                            "  end_time     DATETIME, " +
                            "  status       VARCHAR(20) DEFAULT 'OPEN', " +
                            "  winner_id    VARCHAR(36), " +
                            "  final_price  DOUBLE DEFAULT 0, " +
                            "  FOREIGN KEY (item_id)   REFERENCES items(id) ON DELETE CASCADE, " +
                            "  FOREIGN KEY (winner_id) REFERENCES users(id) ON DELETE SET NULL" +
                            ")"
            );

            // Tạo bảng lịch sử đặt giá
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS bid_transaction (" +
                            "  id          INT AUTO_INCREMENT PRIMARY KEY, " +
                            "  auction_id  INT NOT NULL, " +
                            "  bidder_id   VARCHAR(50) NOT NULL, " +
                            "  bid_amount  DOUBLE NOT NULL, " +
                            "  timestamp   DATETIME NOT NULL, " +
                            "  FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE" +
                            ")"
            );

            System.out.println("\n✅ Thiết lập hoàn tất!");

        } catch (Exception e) {
            System.err.println("❌ Lỗi thiết lập DB: " + e.getMessage());
        }
    }
}