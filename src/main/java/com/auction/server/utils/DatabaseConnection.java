package com.auction.server.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Quản lý kết nối Database sử dụng HikariCP
 * Cung cấp một Pool (hồ chứa) các kết nối, giúp xử lý đa luồng an toàn và tốc độ cao
 */
public class DatabaseConnection {
    // Thông tin kết nối Aiven Cloud MySQL
    private static final String URL = "jdbc:mysql://mysql-24dbe87d-team09-uet.c.aivencloud.com:12014/defaultdb?ssl-mode=REQUIRED";
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "AVNS_DxKGxvHASiK6mmOKQbm";

    private static HikariDataSource dataSource;

    // Khối static được chạy một lần duy nhất khi class được nạp vào bộ nhớ
    static {
        try {
            System.out.println(">>> Đang thiết lập Connection Pool (HikariCP)...");
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(URL);
            config.setUsername(USER);
            config.setPassword(PASSWORD);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Cấu hình tối ưu cho môi trường đa luồng (Server)
            config.setMaximumPoolSize(20);      // Tối đa 20 luồng (client) có thể truy vấn cùng lúc
            config.setMinimumIdle(5);           // Luôn giữ ít nhất 5 kết nối sẵn sàng
            config.setIdleTimeout(30000);       // Đóng kết nối nếu k dùng sau 30 giây
            config.setMaxLifetime(1800000);     // Đóng và tạo lại kết nối sau 30 phút để tránh lỗi mạng
            config.setConnectionTimeout(10000); // Ném lỗi nếu chờ 10 giây mà không lấy được kết nối

            dataSource = new HikariDataSource(config);
            System.out.println(">>> Thiết lập Connection Pool thành công!");
        } catch (Exception e) {
            System.err.println("[DB Error] Lỗi khi khởi tạo HikariCP: " + e.getMessage());
            throw new RuntimeException("Không thể khởi tạo Database Pool", e);
        }
    }

    private DatabaseConnection() {
        // Private constructor để ngăn không cho khởi tạo đối tượng
    }

    /**
     * Lấy 1 kết nối từ trong Pool.
     * Hàm gọi  bắt buộc phải đóng (close) kết nối sau khi dùng xong
     * để trả kết nối về lại Pool cho luồng khác dùng
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Đóng toàn bộ Pool (Chỉ gọi khi tắt Server hoàn toàn)
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println(">>> Đã đóng Connection Pool");
        }
    }
}