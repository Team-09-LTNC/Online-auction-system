package com.auction.server.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://mysql-24dbe87d-team09-uet.c.aivencloud.com:12014/defaultdb?ssl-mode=REQUIRED";
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "AVNS_DxKGxvHASiK6mmOKQbm";

    private static Connection connection = null;

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.out.println(">>> Đang tạo kết nối mới tới Database...");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println(">>> Kết nối Database thành công!");
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi kết nối Cloud: " + e.getMessage());
            try {
                System.out.println(">>> Đang thử reconnect...");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println(">>> Reconnect thành công!");
            } catch (SQLException ex) {
                System.err.println("❌ Reconnect thất bại: " + ex.getMessage());
                connection = null;
            }
        }
        return connection;
    }
}