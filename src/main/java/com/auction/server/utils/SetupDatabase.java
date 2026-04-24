package com.auction.server.utils;

import java.sql.Connection;
import java.sql.Statement;

public class SetupDatabase {
    public static void main(String[] args) {
        System.out.println("⏳ Đang thiết lập cơ sở dữ liệu...");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Tạo bảng người dùng (Lưu ý: ID dùng VARCHAR(36) để khớp với UUID trong Entity)
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "username VARCHAR(50) UNIQUE NOT NULL, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "full_name VARCHAR(100), " +
                    "role ENUM('BIDDER', 'SELLER', 'ADMIN') NOT NULL)";
            stmt.execute(createUsersTable);
            System.out.println("✅ Đã tạo bảng 'users'.");

            // Tạo bảng items (Vật phẩm đấu giá)
            String createItemsTable = "CREATE TABLE IF NOT EXISTS items (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "category ENUM('ELECTRONICS', 'ART', 'VEHICLE') NOT NULL, " +
                    "starting_price DOUBLE NOT NULL, " +
                    "current_price DOUBLE DEFAULT 0, " +
                    "status VARCHAR(20) DEFAULT 'OPEN')";
            stmt.execute(createItemsTable);
            System.out.println("✅ Đã tạo bảng 'items'.");

            System.out.println("🎉 Hoàn tất thiết lập! Hãy quay lại chạy ứng dụng.");
        } catch (Exception e) {
            System.err.println("❌ Lỗi thiết lập DB: " + e.getMessage());
        }
    }
}