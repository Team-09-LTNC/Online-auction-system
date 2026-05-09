package com.auction.server.db;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Script khởi tạo Database
 * Chỉ bao gồm 4 bảng: users, items, auctions, bid_history theo đúng thiết kế.
 */
public class SetupDatabase {
    public static void main(String[] args) {
        System.out.println(">>> BẮT ĐẦU QUY TRÌNH KHỞI TẠO DATABASE...");


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

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

        } catch (Exception e) {
            System.err.println("\n>>> [LỖI] Không thể khởi tạo database:");
            e.printStackTrace();
        }
    }
}