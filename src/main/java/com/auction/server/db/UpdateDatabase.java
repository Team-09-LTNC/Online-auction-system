package com.auction.server.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Script cập nhật Database:
 * Tự động kiểm tra và thêm cấu trúc mới (Cột status, Chỉ mục Index)
 * Đảm bảo an toàn tuyệt đối cho dữ liệu hiện tại, có thể chạy lại nhiều lần không lỗi.
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

                // 2. Tự động kiểm tra xem Index cho cột 'status' đã tồn tại chưa để tối ưu hóa tốc độ Login
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
            }

            logger.info("=== QUY TRÌNH CẬP NHẬT HOÀN THÀNH AN TOÀN ===");

        } catch (Exception e) {
            logger.error(">>> Lỗi nghiêm trọng khi cập nhật cấu trúc Database!", e);
        }
    }
}