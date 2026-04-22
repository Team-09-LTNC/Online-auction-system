package com.auction.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Lưu ý chữ online_auction_system ở cuối chính là tên Database của team
    private static final String URL = "jdbc:mysql://localhost:3306/online_auction_system";
    private static final String USER = "root";

    // Thay đoạn này bằng mật khẩu MySQL để chạy!
    private static final String PASSWORD = "Nhập mật khẩu của bạn vào đây!";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("❌ Sập nguồn! Lỗi kết nối Database: " + e.getMessage());
            return null;
        }
    }
}