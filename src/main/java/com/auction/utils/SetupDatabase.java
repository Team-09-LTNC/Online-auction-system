package com.auction.utils;

import java.sql.Connection;
import java.sql.Statement;

public class SetupDatabase {
    public static void main(String[] args) {
        System.out.println("⏳ Đang kết nối lên Aiven Cloud để tạo bảng...");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Tạo bảng Items trước (Bắt buộc để làm khóa ngoại)
            String createItemsTable = "CREATE TABLE IF NOT EXISTS items (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "category ENUM('ELECTRONICS', 'ART', 'VEHICLE') NOT NULL, " +
                    "starting_price DOUBLE NOT NULL, " +
                    "current_price DOUBLE DEFAULT 0, " +
                    "status VARCHAR(20) DEFAULT 'OPEN')";
            stmt.execute(createItemsTable);
            System.out.println("✅ Đã tạo/kiểm tra xong bảng 'items'.");

            // 2. Tạo bảng bid_transaction
            String createBidTable = "CREATE TABLE IF NOT EXISTS bid_transaction (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "auction_id INT NOT NULL, " +
                    "bidder_id VARCHAR(50) NOT NULL, " +
                    "bid_amount DOUBLE NOT NULL, " +
                    "timestamp TIMESTAMP NOT NULL, " +
                    "FOREIGN KEY (auction_id) REFERENCES items(id) ON DELETE CASCADE)";
            stmt.execute(createBidTable);
            System.out.println("✅ Đã tạo/kiểm tra xong bảng 'bid_transaction'.");

            // 3. Chèn vật phẩm mẫu ID 1 để Main.java có thể đấu giá
            String insertDummyItem = "INSERT INTO items (id, name, category, starting_price, status) " +
                    "VALUES (1, 'Buc Tranh Mona Lisa', 'ART', 1000.0, 'RUNNING') " +
                    "ON DUPLICATE KEY UPDATE name=name";
            stmt.execute(insertDummyItem);
            System.out.println("✅ Đã chèn vật phẩm mẫu thành công.");

            System.out.println("🎉 HOÀN TẤT! BẠN HÃY QUAY LẠI CHẠY FILE MAIN.JAVA NGAY NHÉ!");

        } catch (Exception e) {
            System.err.println("❌ Lỗi nghiêm trọng khi tạo bảng: " + e.getMessage());
            e.printStackTrace();
        }
    }
}