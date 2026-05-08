package com.auction.server.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Lớp dùng để khởi tạo toàn bộ Cấu trúc Cơ sở dữ liệu (Schema) trên Azure
 * Chỉ cần chạy 1 lần duy nhất để tạo các bảng
 */
public class SetupDatabase {
    public static void main(String[] args) {
        System.out.println(">>> Đang kết nối tới Azure Database để khởi tạo các bảng...");

        // Sử dụng ConnectionProvider
        ConnectionProvider provider = DatabaseConnection.getInstance();

        // Sử dụng try-with-resources để tự động đóng kết nối
        try (Connection conn = provider.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Tạo bảng USERS (Bảng gốc - Không phụ thuộc ai)
            String createUsers = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "full_name VARCHAR(100) NOT NULL, " +
                    "role ENUM('ADMIN', 'SELLER', 'BIDDER') NOT NULL, " +
                    "balance BIGINT DEFAULT 0" +
                    ")";
            stmt.execute(createUsers);
            System.out.println("[OK] Đã tạo bảng 'users'");

            // 2. Tạo bảng ITEMS (Phụ thuộc vào bảng users qua seller_id)
            String createItems = "CREATE TABLE IF NOT EXISTS items (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "seller_id INT NOT NULL, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "description TEXT, " +
                    "category VARCHAR(50) NOT NULL, " +
                    "starting_price BIGINT NOT NULL, " +
                    "image_url VARCHAR(500), " +
                    "FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE" +
                    ")";
            stmt.execute(createItems);
            System.out.println("[OK] Đã tạo bảng 'items'");

            // 3. Tạo bảng AUCTIONS (Phụ thuộc vào items và users)
            String createAuctions = "CREATE TABLE IF NOT EXISTS auctions (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "item_id INT NOT NULL, " +
                    "current_price BIGINT NOT NULL, " +
                    "highest_bidder_id INT, " +
                    "start_time DATETIME NOT NULL, " +
                    "end_time DATETIME NOT NULL, " +
                    "status VARCHAR(30) NOT NULL, " +
                    "FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (highest_bidder_id) REFERENCES users(id) ON DELETE SET NULL" +
                    ")";
            stmt.execute(createAuctions);
            System.out.println("[OK] Đã tạo bảng 'auctions'");

            // 4. Tạo bảng BID_HISTORY (Phụ thuộc vào auctions và users)
            String createBidHistory = "CREATE TABLE IF NOT EXISTS bid_history (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "auction_id INT NOT NULL, " +
                    "bidder_id INT NOT NULL, " +
                    "bid_amount BIGINT NOT NULL, " +
                    "bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE" +
                    ")";
            stmt.execute(createBidHistory);
            System.out.println("[OK] Đã tạo bảng 'bid_history'");

            System.out.println(">>> KHỞI TẠO CƠ SỞ DỮ LIỆU THÀNH CÔNG! BẠN CÓ THỂ BẮT ĐẦU CHẠY SERVER.");

        } catch (SQLException e) {
            System.err.println(">>> [LỖI] Khởi tạo Database thất bại: " + e.getMessage());
            e.printStackTrace();
        }
    }
}