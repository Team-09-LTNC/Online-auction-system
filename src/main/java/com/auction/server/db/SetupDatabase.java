package com.auction.server.db;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Script khởi tạo Database CHUẨN:
 * Chỉ bao gồm 4 bảng: users, items, auctions, bid_history theo đúng thiết kế.
 */
public class SetupDatabase {
    public static void main(String[] args) {
        System.out.println(">>> BẮT ĐẦU QUY TRÌNH KHỞI TẠO DATABASE...");

        // --- 1. Lệnh TẠO BẢNG (Chuẩn 4 bảng) ---

        // Bảng 1: Users
        String createUsersTable = "CREATE TABLE users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "password VARCHAR(255) NOT NULL, " +
                "full_name VARCHAR(100) NOT NULL, " +
                "role ENUM('ADMIN', 'SELLER', 'BIDDER') NOT NULL, " +
                "balance BIGINT DEFAULT 0" +
                ") ENGINE=InnoDB;";

        // Bảng 2: Items
        String createItemsTable = "CREATE TABLE items (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "seller_id INT NOT NULL, " +
                "name VARCHAR(255) NOT NULL, " +
                "description TEXT, " +
                "category VARCHAR(50), " +
                "starting_price BIGINT NOT NULL, " +
                "bid_increment BIGINT NOT NULL, " +
                "image_url VARCHAR(255), " +
                "FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB;";

        // Bảng 3: Auctions
        String createAuctionsTable = "CREATE TABLE auctions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "item_id INT NOT NULL, " +
                "current_price BIGINT NOT NULL, " +
                "highest_bidder_id INT, " +
                "start_time DATETIME NOT NULL, " +
                "end_time DATETIME NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'OPEN', " +
                "FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (highest_bidder_id) REFERENCES users(id) ON DELETE SET NULL" +
                ") ENGINE=InnoDB;";

        // Bảng 4: Bid History
        String createBidHistoryTable = "CREATE TABLE bid_history (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "auction_id INT NOT NULL, " +
                "bidder_id INT NOT NULL, " +
                "bid_amount BIGINT NOT NULL, " +
                "bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB;";


        // --- 2. THỰC THI SQL ---
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println(">>> Đang dọn dẹp môi trường (bao gồm cả tàn dư cũ)...");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");

            // Xóa dứt điểm tàn dư cũ (nếu còn trên Aiven) để tránh lỗi
            stmt.execute("DROP TABLE IF EXISTS bid_transaction;");

            // Xóa 4 bảng chính
            stmt.execute("DROP TABLE IF EXISTS bid_history;");
            stmt.execute("DROP TABLE IF EXISTS auctions;");
            stmt.execute("DROP TABLE IF EXISTS items;");
            stmt.execute("DROP TABLE IF EXISTS users;");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1;");

            System.out.println(">>> Đang tạo bảng 'users'...");
            stmt.execute(createUsersTable);

            System.out.println(">>> Đang tạo bảng 'items'...");
            stmt.execute(createItemsTable);

            System.out.println(">>> Đang tạo bảng 'auctions'...");
            stmt.execute(createAuctionsTable);

            System.out.println(">>> Đang tạo bảng 'bid_history'...");
            stmt.execute(createBidHistoryTable);

            System.out.println("\n==================================================");
            System.out.println(">>> THIẾT LẬP DATABASE MỚI THÀNH CÔNG CHỈ VỚI 4 BẢNG!");
            System.out.println("==================================================");

        } catch (Exception e) {
            System.err.println("\n>>> [LỖI] Không thể khởi tạo database:");
            e.printStackTrace();
        }
    }
}