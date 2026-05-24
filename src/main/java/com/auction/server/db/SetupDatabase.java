package com.auction.server.db;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Script khởi tạo Database CHUẨN:
 * Bao gồm đầy đủ các bảng và cột cần thiết cho hệ thống đấu giá.
 * Đã tích hợp bộ dữ liệu mẫu (Seeder) đa dạng cho UI test.
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

        // Bảng 3: Auctions (ĐÃ THÊM CỘT buy_now_price)
        String createAuctionsTable = "CREATE TABLE auctions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "item_id INT NOT NULL, " +
                "current_price BIGINT NOT NULL DEFAULT 0, " +
                "buy_now_price BIGINT DEFAULT NULL, " + // <-- Cột giá mua đứt
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
                "recipient_id INT, " +
                "message TEXT NOT NULL, " +
                "payment_required BOOLEAN NOT NULL DEFAULT FALSE, " +
                "is_read BOOLEAN NOT NULL DEFAULT FALSE, " +
                "send_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE" +
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

            System.out.println(">>> Đang nạp dữ liệu mẫu (Users)...");
            stmt.execute("INSERT INTO users (id, username, password, full_name, role, balance) VALUES " +
                    "(1, 'admin', 'admin', 'Quản trị viên', 'ADMIN', 0), " +
                    "(2, 'seller1', '123456', 'Người Bán Số 1', 'SELLER', 5000000), " +
                    "(3, 'bidder1', '123456', 'Người Mua Số 1', 'BIDDER', 150000000), " +
                    "(4, 'bidder2', '123456', 'Người Mua Số 2', 'BIDDER', 200000000);");

            System.out.println(">>> Đang nạp dữ liệu mẫu (Items)...");
            stmt.execute("INSERT INTO items (id, seller_id, name, description, category, starting_price, bid_increment, image_url) VALUES " +
                    // --- DANH MỤC: XE CỘ (VEHICLE) ---
                    "(1, 2, 'Mercedes-Benz S450 2023', 'Xe sang lướt 5000km, màu đen nội thất kem.', 'VEHICLE', 3000000000, 10000000, 'https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?w=800'), " +
                    "(2, 2, 'Porsche 911 GT3 RS', 'Siêu xe thể thao nhập khẩu nguyên chiếc từ Đức.', 'VEHICLE', 8500000000, 50000000, 'https://images.unsplash.com/photo-1503376713295-8bc2584400f9?w=800'), " +
                    "(3, 2, 'BMW S1000RR 2024', 'Cá mập siêu phân khối, ODO 1000km.', 'VEHICLE', 750000000, 5000000, 'https://images.unsplash.com/photo-1558981403-c5f9899a28bc?w=800'), " +
                    // --- DANH MỤC: ĐIỆN TỬ (ELECTRONICS) ---
                    "(4, 2, 'MacBook Pro M3 Max 16inch', 'Bản max option 128GB RAM, 4TB SSD. Likenew 99%.', 'ELECTRONICS', 120000000, 1000000, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800'), " +
                    "(5, 2, 'Sony A7 IV Camera & Lens', 'Máy ảnh mirrorless chuyên nghiệp kèm ống kính 24-70mm f/2.8.', 'ELECTRONICS', 65000000, 500000, 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800'), " +
                    "(6, 2, 'iPhone 15 Pro Max 1TB', 'Màu Titan Tự Nhiên, pin 100%, bảo hành Apple Care+ 2025.', 'ELECTRONICS', 35000000, 500000, 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800'), " +
                    // --- DANH MỤC: TRANH ẢNH & NGHỆ THUẬT (ART) ---
                    "(7, 2, 'Tranh Sơn Dầu: Đêm Đầy Sao', 'Bản sao chép cao cấp, kích thước 100x150cm, có khung gỗ sồi.', 'ART', 15000000, 500000, 'https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=800'), " +
                    "(8, 2, 'Tượng Thạch Cao Cổ Điển', 'Tượng điêu khắc lấy cảm hứng từ thời kỳ Phục Hưng.', 'ART', 25000000, 1000000, 'https://images.unsplash.com/photo-1544531586-fde5298cdd40?w=800'), " +
                    "(9, 2, 'Bình Gốm Cổ Men Rạn', 'Đồ cổ thế kỷ 19, bảo quản hoàn hảo không tì vết.', 'ART', 150000000, 5000000, 'https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=800');"
            );

            System.out.println(">>> Đang nạp dữ liệu mẫu (Auctions)...");
            stmt.execute("INSERT INTO auctions (id, item_id, current_price, buy_now_price, status, start_time, end_time) VALUES " +
                    // Trạng thái RUNNING: Đang diễn ra
                    "(1, 1, 3000000000, 3500000000, 'RUNNING', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR)), " +
                    "(4, 4, 125000000,  150000000,  'RUNNING', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 5 HOUR)), " +
                    "(7, 7, 18000000,   NULL,       'RUNNING', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 12 HOUR)), " +

                    // Trạng thái OPEN: Sắp diễn ra
                    "(2, 2, 8500000000, 9500000000, 'OPEN', DATE_ADD(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 24 HOUR)), " +
                    "(5, 5, 65000000,   NULL,       'OPEN', DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 48 HOUR)), " +
                    "(8, 8, 25000000,   35000000,   'OPEN', DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 72 HOUR)), " +

                    // Trạng thái FINISHED: Đã kết thúc (Cập nhật từ CLOSED -> FINISHED)
                    "(3, 3, 820000000,  900000000,  'FINISHED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)), " +
                    "(6, 6, 38500000,   45000000,   'FINISHED', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)), " +
                    "(9, 9, 210000000,  NULL,       'FINISHED', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY));"
            );

            System.out.println("\n==================================================");
            System.out.println(">>> THIẾT LẬP DATABASE MỚI THÀNH CÔNG VỚI ĐẦY ĐỦ CÁC BẢNG & DATA!");
            System.out.println("==================================================");

        } catch (Exception e) {
            System.err.println("\n>>> [LỖI] Không thể khởi tạo database:");
            e.printStackTrace();
        }
    }
}
