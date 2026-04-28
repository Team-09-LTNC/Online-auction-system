package com.auction.server.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Sửa cái dòng URL này trong file DatabaseConnection.java nhé
    private static final String URL = "jdbc:mysql://mysql-24dbe87d-team09-uet.c.aivencloud.com:12014/defaultdb?sslMode=DISABLED&allowPublicKeyRetrieval=true&useSSL=false";
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "uetteam09ltnc@@"; // Token của nhóm

    private static Connection connection = null;

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi kết nối Cloud: " + e.getMessage());
        }
        return connection;
    }
}