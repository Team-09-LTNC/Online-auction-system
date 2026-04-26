package com.auction.server.utils;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Chạy một lần để tạo toàn bộ bảng trong DB.
 * BUG FIX:
 *   - Thêm cột description vào bảng items (trước thiếu, gây lỗi khi saveItem)
 *   - Thêm bảng bid_transaction (trước thiếu hoàn toàn, BidTransactionDAO bị lỗi)
 *   - Thêm bảng auctions để tách item khỏi phiên đấu giá
 */
public class SetupDatabase {
    public static void main(String[] args) {
        System.out.println("Đang thiết lập cơ sở dữ liệu...");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Bảng users
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id         VARCHAR(36)  PRIMARY KEY, " +
                "  username   VARCHAR(50)  UNIQUE NOT NULL, " +
                "  password   VARCHAR(255) NOT NULL, " +
                "  full_name  VARCHAR(100), " +
                "  role       ENUM('BIDDER','SELLER','ADMIN') NOT NULL" +
                ")"
            );

            // Bảng items
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

            // Bảng auctions — liên kết item với phiên đấu giá
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

            // Bảng bid_transaction
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

            System.out.println("\nHoan tat thiet lap! Hay quay lai chay ung dung.");

        } catch (Exception e) {
            System.err.println("Loi thiet lap DB: " + e.getMessage());
        }
    }
}
