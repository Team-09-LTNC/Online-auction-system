package com.auction.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            // 1. Tải cấu hình từ file properties (mặc định application.properties)
            String configFile = System.getProperty("db.config.file", "application.properties");
            Properties props = new Properties();
            try (InputStream input = openConfig(configFile)) {
                if (input == null) {
                    throw new RuntimeException("CRITICAL: Không tìm thấy file cấu hình DB: " + configFile);
                }
                props.load(input);
            }

            // 2. Trích xuất thông tin kết nối
            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String pass = props.getProperty("db.password");

            if (url == null || url.isBlank()) {
                String host = props.getProperty("db.host");
                String port = props.getProperty("db.port");
                String dbName = props.getProperty("db.name");
                boolean defaultSsl = !("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host));
                boolean useSsl = Boolean.parseBoolean(props.getProperty("db.ssl", String.valueOf(defaultSsl)));
                url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                        + (useSsl
                        ? "?useSSL=true&requireSSL=true&trustServerCertificate=true"
                        : "?useSSL=false")
                        + "&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true";
            }

            // 4. Thiết lập HikariConfig
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(pass);
            if (props.getProperty("db.driver") != null) {
                config.setDriverClassName(props.getProperty("db.driver"));
            }
            if (url.startsWith("jdbc:mysql:")) {
                config.setConnectionInitSql("SET time_zone = '+07:00'");
            }

            // Cấu hình tối ưu cho môi trường đa luồng (Concurrency)
            config.setMaximumPoolSize(50);      // Tối đa 50 luồng (client) có thể truy vấn cùng lúc
            config.setMinimumIdle(10);           // Luôn giữ ít nhất 10 kết nối sẵn sàng
            config.setIdleTimeout(30000);       // Đóng kết nối nếu k dùng sau 30 giây
            config.setMaxLifetime(1800000);     // Đóng và tạo lại kết nối sau 30 phút để tránh lỗi mạng
            config.setConnectionTimeout(10000); // Ném lỗi nếu chờ 10 giây mà không lấy được kết nối

            this.dataSource = new HikariDataSource(config);

            // JVM tự động đóng Pool an toàn khi tắt Server
            Runtime.getRuntime().addShutdownHook(new Thread(this::closePool));

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

    private InputStream openConfig(String configFile) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream input = classLoader.getResourceAsStream(configFile);
        if (input != null) {
            return input;
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null && contextClassLoader != classLoader) {
            input = contextClassLoader.getResourceAsStream(configFile);
            if (input != null) {
                return input;
            }
        }

        Path configuredPath = Path.of(configFile);
        if (Files.isRegularFile(configuredPath)) {
            return Files.newInputStream(configuredPath);
        }

        Path testResourcePath = Path.of("src", "test", "resources", configFile);
        if (Files.isRegularFile(testResourcePath)) {
            return Files.newInputStream(testResourcePath);
        }

        Path mainResourcePath = Path.of("src", "main", "resources", configFile);
        if (Files.isRegularFile(mainResourcePath)) {
            return Files.newInputStream(mainResourcePath);
        }

        return null;
    }

    private void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Đã đóng Connection Pool tự động bởi JVM");
        }
    }
}
