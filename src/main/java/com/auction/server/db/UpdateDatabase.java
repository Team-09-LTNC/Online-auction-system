package com.auction.server.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Script cập nhật Database:
 * Tự động kiểm tra và thêm cấu trúc mới (Cột status, Chỉ mục Index, Bảng
 * auto_bid_settings)
 * Đảm bảo an toàn tuyệt đối cho dữ liệu hiện tại, có thể chạy lại nhiều lần
 * không lỗi.
 */
public class UpdateDatabase {
    private static final Logger logger = LoggerFactory.getLogger(UpdateDatabase.class);

    public static void main(String[] args) {
        logger.info(">>> BẮT ĐẦU KIỂM TRA VÀ CẬP NHẬT DATABASE...");

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // 1. Kiểm tra xem cột 'status' đã tồn tại trong bảng 'users' chưa
            boolean columnExists = false;
            try (ResultSet rs = metaData.getColumns(null, null, "users", "status")) {
                if (rs.next()) {
                    columnExists = true;
                }
            }

            try (Statement stmt = conn.createStatement()) {
                // Nếu chưa có cột status thì mới thêm vào để bảo vệ dữ liệu cũ
                if (!columnExists) {
                    logger.info(">>> Đang bổ sung cột 'status' vào bảng 'users'...");
                    String sqlAddColumn = "ALTER TABLE users ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE' AFTER balance;";
                    stmt.execute(sqlAddColumn);
                    logger.info(">>> Thêm cột 'status' thành công! Các user cũ đã được tự động đặt là ACTIVE.");
                } else {
                    logger.info(">>> Cột 'status' đã tồn tại từ trước. Bỏ qua để giữ nguyên dữ liệu.");
                }

                // 2. Tự động kiểm tra xem Index cho cột 'status' đã tồn tại chưa để tối ưu hóa
                // tốc độ Login
                boolean indexExists = false;
                try (ResultSet rs = metaData.getIndexInfo(null, null, "users", false, false)) {
                    while (rs.next()) {
                        String indexName = rs.getString("INDEX_NAME");
                        if ("idx_user_status".equalsIgnoreCase(indexName)) {
                            indexExists = true;
                            break;
                        }
                    }
                }

                // Nếu chưa có Index thì tạo mới để hệ thống đạt tốc độ tối đa
                if (!indexExists) {
                    logger.info(">>> Đang khởi tạo chỉ mục tốc độ cao idx_user_status...");
                    String sqlCreateIndex = "CREATE INDEX idx_user_status ON users(status);";
                    stmt.execute(sqlCreateIndex);
                    logger.info(">>> Tạo Index tối ưu hóa truy vấn thành công!");
                } else {
                    logger.info(">>> Chỉ mục idx_user_status đã sẵn sàng, không cần tạo lại.");
                }

                // 3. Kiểm tra xem bảng 'auto_bid_settings' đã tồn tại chưa
                boolean tableAutoBidExists = false;
                try (ResultSet rs = metaData.getTables(null, null, "auto_bid_settings", null)) {
                    if (rs.next()) {
                        tableAutoBidExists = true;
                    }
                }

                // Nếu chưa có thì tạo bảng mới để lưu cấu hình đấu giá tự động
                if (!tableAutoBidExists) {
                    logger.info(">>> Đang tạo bảng 'auto_bid_settings' để lưu giá trần tự động...");
                    String sqlCreateAutoBidTable = "CREATE TABLE auto_bid_settings (" +
                            "auction_id INT NOT NULL, " +
                            "bidder_id INT NOT NULL, " +
                            "max_auto_bid BIGINT NOT NULL, " +
                            "register_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                            "PRIMARY KEY (auction_id, bidder_id), " + // Đảm bảo mỗi user chỉ có 1 max_bid cho 1 phiên
                            "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE, " +
                            "FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB;";
                    stmt.execute(sqlCreateAutoBidTable);
                    logger.info(">>> Tạo bảng 'auto_bid_settings' thành công!");
                } else {
                    logger.info(">>> Bảng 'auto_bid_settings' đã tồn tại.");
                }

                // 4. Kiểm tra enum status của bảng auctions có chứa PENDING chưa
                boolean hasPending = false;
                try (ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT COLUMN_TYPE FROM information_schema.COLUMNS " +
                                "WHERE TABLE_NAME = 'auctions' AND COLUMN_NAME = 'status'")) {
                    if (rs.next()) {
                        String columnType = rs.getString("COLUMN_TYPE");
                        hasPending = columnType.contains("PENDING");
                    }
                }

                if (!hasPending) {
                    logger.info(">>> Đang cập nhật enum status cho bảng 'auctions'...");
                    stmt.execute("ALTER TABLE auctions MODIFY COLUMN status " +
                            "ENUM('PENDING', 'OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED', 'REJECTED') " +
                            "DEFAULT 'PENDING'");
                    logger.info(">>> Cập nhật enum status thành công!");
                } else {
                    logger.info(">>> Enum status bảng 'auctions' đã có PENDING. Bỏ qua.");
                }
            }

            logger.info("=== QUY TRÌNH CẬP NHẬT HOÀN THÀNH AN TOÀN ===");

        } catch (Exception e) {
            logger.error(">>> Lỗi nghiêm trọng khi cập nhật cấu trúc Database!", e);
        }
    }
}