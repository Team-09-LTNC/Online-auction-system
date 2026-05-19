package com.auction.server.db;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Script khởi tạo Database CHUẨN:
 * Bao gồm đầy đủ các bảng và cột cần thiết cho hệ thống đấu giá.
 */
public class SetupDatabase {
    public static void main(String[] args) {
        System.out.println(">>> BẮT ĐẦU QUY TRÌNH KHỞI TẠO DATABASE...");

        // --- 1. Lệnh TẠO BẢNG ---

        // Bảng 1: Users (Thêm cột balance)
        String createUsersTable = "CREATE TABLE users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "password VARCHAR(255) NOT NULL, " +
                "full_name VARCHAR(100) NOT NULL, " +
                "role ENUM('ADMIN', 'SELLER', 'BIDDER') NOT NULL, " +
                "balance BIGINT DEFAULT 0" +
                ") ENGINE=InnoDB;";

        // Bảng 2: Items (Thêm cột image_url)
        String createItemsTable = "CREATE TABLE items (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "seller_id INT NOT NULL, " +
                "name VARCHAR(255) NOT NULL, " +
                "description TEXT, " +
                "category VARCHAR(50), " +
                "starting_price BIGINT NOT NULL, " +
                "bid_increment BIGINT NOT NULL DEFAULT 100000, " +
                "image_url VARCHAR(500), " +
                "FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB;";

        // Bảng 3: Auctions
        String createAuctionsTable = "CREATE TABLE auctions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "item_id INT NOT NULL, " +
                "current_price BIGINT NOT NULL DEFAULT 0, " +
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

        // Bảng 5: Follows
        String createFollowsTable = "CREATE TABLE follows (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "auction_id INT NOT NULL, " +
                "UNIQUE KEY unique_follow (user_id, auction_id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB;";

        // Bảng 6: Chat Messages
        String createChatMessagesTable = "CREATE TABLE chat_messages (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "auction_id INT NOT NULL, " +
                "sender_id INT NOT NULL, " +
                "message TEXT NOT NULL, " +
                "send_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB;";

        // Bảng 7: Wallet Transactions (Lịch sử biến động số dư)
        String createWalletTransactionsTable = "CREATE TABLE wallet_transactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "transaction_type ENUM('DEPOSIT', 'WITHDRAW', 'PAYMENT_SENT', 'PAYMENT_RECEIVED') NOT NULL, " +
                "amount BIGINT NOT NULL, " +
                "description VARCHAR(255), " +
                "transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB;";


        // --- 2. THỰC THI SQL ---
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println(">>> Đang dọn dẹp môi trường (bao gồm cả tàn dư cũ)...");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");

            stmt.execute("DROP TABLE IF EXISTS wallet_transactions;");
            stmt.execute("DROP TABLE IF EXISTS chat_messages;");
            stmt.execute("DROP TABLE IF EXISTS follows;");
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

            System.out.println(">>> Đang tạo bảng 'follows'...");
            stmt.execute(createFollowsTable);

            System.out.println(">>> Đang tạo bảng 'chat_messages'...");
            stmt.execute(createChatMessagesTable);

            System.out.println(">>> Đang tạo bảng 'wallet_transactions'...");
            stmt.execute(createWalletTransactionsTable);

            System.out.println(">>> Đang nạp dữ liệu mẫu để Test...");
            stmt.execute("INSERT INTO users (id, username, password, full_name, role, balance) VALUES " +
                    "(1, 'admin', 'admin', 'Quản trị viên', 'ADMIN', 0), " +
                    "(2, 'seller1', '123456', 'Người Bán Số 1', 'SELLER', 5000000), " +
                    "(3, 'bidder1', '123456', 'Người Mua Số 1', 'BIDDER', 150000000), " +
                    "(4, 'bidder2', '123456', 'Người Mua Số 2', 'BIDDER', 200000000);");

            stmt.execute("INSERT INTO items (id, seller_id, name, description, category, starting_price, bid_increment, image_url) VALUES " +
                    "(1, 2, 'Mercedes-Benz S450', 'Xe sang lướt 5000km', 'VEHICLE', 3000000000, 1000000, 'https://img.freepik.com/free-photo/mercedes-benz-s-class-driving-down-empty-road_114579-22340.jpg'), " +
                    "(2, 2, 'MacBook Pro M3 Max', 'Máy likenew', 'ELECTRONICS', 80000000, 500000, ''), " +
                    "(3, 2, 'Tranh Cổ', 'Nghệ thuật nguyên bản', 'ART', 50000000, 1000000, '');");

            stmt.execute("INSERT INTO auctions (id, item_id, current_price, status, start_time, end_time) VALUES " +
                    "(1, 1, 3000000000, 'RUNNING', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 1 HOUR)), " +
                    "(2, 2, 80000000, 'RUNNING', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 12 HOUR)), " +
                    "(3, 3, 50000000, 'OPEN', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY));");

            System.out.println("\n==================================================");
            System.out.println(">>> THIẾT LẬP DATABASE MỚI THÀNH CÔNG VỚI ĐẦY ĐỦ CÁC BẢNG & DATA!");
            System.out.println("==================================================");

        } catch (Exception e) {
            System.err.println("\n>>> [LỖI] Không thể khởi tạo database:");
            e.printStackTrace();
        }
    }
}