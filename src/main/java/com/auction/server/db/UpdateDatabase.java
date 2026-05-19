package com.auction.server.db;

import java.sql.Connection;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; 
/**
 * Script cập nhật Database:
 * Thêm cột status vào bảng users để quản lý trạng thái người dùng (ACTIVE, INACTIVE, BANNED).
 */

public class UpdateDatabase {
    private static final Logger logger = LoggerFactory.getLogger(UpdateDatabase.class);

    public static void main(String[] args) {

        String sql = """
            ALTER TABLE users
            ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE' AFTER balance;
        """;

        try (
                Connection conn = DatabaseConnection.getInstance().getConnection();
                Statement stmt = conn.createStatement()
        ) {

            stmt.execute(sql);

            logger.info(">>> Thêm cột status thành công!");

        } catch (Exception e) {
            logger.error(">>> Lỗi khi thêm cột status!", e);
        }
    }
}