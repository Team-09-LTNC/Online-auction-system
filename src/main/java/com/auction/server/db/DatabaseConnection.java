package com.auction.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Quản lý kết nối Database sử dụng HikariCP áp dụng Singleton Pattern
 * cấu hình kết nối tới Microsoft Azure Database
 */
public class DatabaseConnection implements ConnectionProvider {

    // Áp dụng Singleton Pattern (Thread-safe)
    private static volatile DatabaseConnection instance;
    private HikariDataSource dataSource;

    // Constructor private để ngăn khởi tạo từ bên ngoài
    private DatabaseConnection() {
        System.out.println(">>> Đang thiết lập Connection Pool (HikariCP) tới Aiven...");
        try {
            HikariConfig config = new HikariConfig();

            // 1. Cấu hình Aiven MySQL
            String host = "mysql-24dbe87d-team09-uet.c.aivencloud.com";
            String port = "12014";
            String dbName = "defaultdb";

            // Chuỗi URL đã tích hợp SSL và bỏ qua kiểm tra chứng chỉ thủ công
            String defaultUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName +
                    "?useSSL=true" +
                    "&requireSSL=true" +
                    "&trustServerCertificate=true" +
                    "&serverTimezone=UTC" +
                    "&allowPublicKeyRetrieval=true";

            String defaultUser = "avnadmin";
            String defaultPass = "AVNS_DxKGxvHASiK6mmOKQbm";

            // 3. NẠP CẤU HÌNH
            config.setJdbcUrl(defaultUrl);
            config.setUsername(defaultUser);
            config.setPassword(defaultPass);
            // Cấu hình tối ưu cho môi trường đa luồng (Server)
            config.setMaximumPoolSize(20);      // Tối đa 20 luồng (client) có thể truy vấn cùng lúc
            config.setMinimumIdle(5);           // Luôn giữ ít nhất 5 kết nối sẵn sàng
            config.setIdleTimeout(30000);       // Đóng kết nối nếu k dùng sau 30 giây
            config.setMaxLifetime(1800000);     // Đóng và tạo lại kết nối sau 30 phút để tránh lỗi mạng
            config.setConnectionTimeout(10000); // Ném lỗi nếu chờ 10 giây mà không lấy được kết nối

            this.dataSource = new HikariDataSource(config);

            // JVM sẽ tự động đóng Pool an toàn khi tắt Server
            Runtime.getRuntime().addShutdownHook(new Thread(this::closePool));

            System.out.println(">>> Thiết lập Connection Pool tới Azure thành công!");
        } catch (Exception e) {
            System.err.println("[DB Error] Lỗi khi khởi tạo HikariCP: " + e.getMessage());
            throw new RuntimeException("Không thể khởi tạo Database Pool", e);
        }
    }

    /**
     * Lấy instance duy nhất của DatabaseConnection
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println(">>> Đã đóng Connection Pool tự động bởi JVM");
        }
    }
}