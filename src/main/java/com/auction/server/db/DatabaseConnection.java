package com.auction.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DatabaseConnection: Quản lý kết nối CSDL sử dụng HikariCP (Singleton Pattern).
 * Đã tích hợp tính năng đọc cấu hình bảo mật từ file properties.
 */
public class DatabaseConnection implements ConnectionProvider {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static volatile DatabaseConnection instance;
    private HikariDataSource dataSource;

    private DatabaseConnection() {
        logger.info("Đang khởi tạo Connection Pool (HikariCP)...");
        try {
            // 1. Tải cấu hình từ file application.properties
            Properties props = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
                if (input == null) {
                    throw new RuntimeException("CRITICAL: Không tìm thấy file application.properties trong thư mục resources!");
                }
                props.load(input);
            }

            // 2. Trích xuất thông tin kết nối
            String host = props.getProperty("db.host");
            String port = props.getProperty("db.port");
            String dbName = props.getProperty("db.name");
            String user = props.getProperty("db.user");
            String pass = props.getProperty("db.password");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName +
                    "?useSSL=true&requireSSL=true&trustServerCertificate=true&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true";

            // 4. Thiết lập HikariConfig
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(pass);

            // Cấu hình tối ưu cho môi trường đa luồng (Concurrency)
            config.setMaximumPoolSize(50);      // Tối đa 50 luồng (client) có thể truy vấn cùng lúc
            config.setMinimumIdle(10);           // Luôn giữ ít nhất 10 kết nối sẵn sàng
            config.setIdleTimeout(30000);       // Đóng kết nối nếu k dùng sau 30 giây
            config.setMaxLifetime(1800000);     // Đóng và tạo lại kết nối sau 30 phút để tránh lỗi mạng
            config.setConnectionTimeout(10000); // Ném lỗi nếu chờ 10 giây mà không lấy được kết nối

            this.dataSource = new HikariDataSource(config);

            // JVM tự động đóng Pool an toàn khi tắt Server
            Runtime.getRuntime().addShutdownHook(new Thread(this::dongPool));

            logger.info("Thiết lập Connection Pool tới Database thành công!");
        } catch (Exception e) {
            logger.error("Lỗi nghiêm trọng khi khởi tạo Database Pool", e);
            throw new RuntimeException("Không thể khởi tạo Database Pool", e);
        }
    }

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

    private void dongPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Đã đóng Connection Pool tự động bởi JVM");
        }
    }
}