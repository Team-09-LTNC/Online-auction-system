package com.auction.server.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Script cập nhật Database:
 * Tự động kiểm tra và thêm cấu trúc mới (Cột status, Chỉ mục Index, Bảng auto_bid_settings)
 * Đảm bảo an toàn tuyệt đối cho dữ liệu hiện tại, có thể chạy lại nhiều lần không lỗi.
 */
public class UpdateDatabase {
    private static final Logger logger = LoggerFactory.getLogger(UpdateDatabase.class);

    public static void main(String[] args) {
        logger.info(">>> BẮT ĐẦU KIỂM TRA VÀ CẬP NHẬT DATABASE...");

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // 1. Kiểm tra xem cột 'status' đã tồn tại trong bảng 'users' chưa
            boolean columnExists = cotTonTai(metaData, "users", "status");

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

                // 4. Đảm bảo phiên seed mua đứt chạy được trên database cũ
                if (!cotTonTai(metaData, "auctions", "buy_now_price")) {
                    logger.info(">>> Đang bổ sung cột 'buy_now_price' vào bảng 'auctions'...");
                    stmt.execute("ALTER TABLE auctions ADD COLUMN buy_now_price BIGINT DEFAULT NULL AFTER current_price;");
                    logger.info(">>> Thêm cột 'buy_now_price' thành công.");
                } else {
                    logger.info(">>> Cột 'buy_now_price' đã tồn tại. Bỏ qua.");
                }

                // 5. Notification riêng dùng lại bảng chat_messages theo từng người nhận
                if (!cotTonTai(metaData, "chat_messages", "recipient_id")) {
                    logger.info(">>> Đang bổ sung cột 'recipient_id' vào bảng 'chat_messages'...");
                    stmt.execute("ALTER TABLE chat_messages ADD COLUMN recipient_id INT NULL AFTER sender_id;");
                    stmt.execute("ALTER TABLE chat_messages ADD CONSTRAINT fk_chat_recipient "
                            + "FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE;");
                } else {
                    logger.info(">>> Cột 'recipient_id' của chat_messages đã tồn tại. Bỏ qua.");
                }

                if (!cotTonTai(metaData, "chat_messages", "payment_required")) {
                    logger.info(">>> Đang bổ sung cột 'payment_required' vào bảng 'chat_messages'...");
                    stmt.execute("ALTER TABLE chat_messages ADD COLUMN payment_required BOOLEAN NOT NULL "
                            + "DEFAULT FALSE AFTER message;");
                } else {
                    logger.info(">>> Cột 'payment_required' của chat_messages đã tồn tại. Bỏ qua.");
                }

                if (!chiMucTonTai(metaData, "chat_messages", "idx_chat_recipient_time")) {
                    logger.info(">>> Đang tạo chỉ mục notification theo người nhận...");
                    stmt.execute("CREATE INDEX idx_chat_recipient_time "
                            + "ON chat_messages(recipient_id, send_time);");
                } else {
                    logger.info(">>> Chỉ mục notification theo người nhận đã tồn tại. Bỏ qua.");
                }
            }

            // Gọi hàm tạo dữ liệu mẫu
            seedPhienMuaDut48Gio(conn);
            seedPhienMuaDutMacBook(conn); // <--- Đã thêm hàm seed sản phẩm thứ 2
            
            logger.info("=== QUY TRÌNH CẬP NHẬT HOÀN THÀNH AN TOÀN ===");

        } catch (Exception e) {
            logger.error(">>> Lỗi nghiêm trọng khi cập nhật cấu trúc Database!", e);
        }
    }

    private static boolean cotTonTai(DatabaseMetaData metaData, String tableName, String columnName)
            throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private static boolean chiMucTonTai(DatabaseMetaData metaData, String tableName, String indexName)
            throws SQLException {
        try (ResultSet rs = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- SEED LEICA Q3 ---
    private static void seedPhienMuaDut48Gio(Connection conn) throws SQLException {
        int sellerId = laySellerId(conn);
        if (sellerId == -1) {
            logger.warn(">>> Không tìm thấy seller để tạo phiên. Bỏ qua seed.");
            return;
        }

        int itemId = layHoacTaoLeicaQ3(conn, sellerId);
        if (daCoPhienChoItem(conn, itemId)) {
            logger.info(">>> Phiên mẫu Leica Q3 đã tồn tại. Không tạo bản trùng.");
            return;
        }

        String insertAuction = "INSERT INTO auctions " +
                "(item_id, current_price, buy_now_price, status, start_time, end_time) " +
                "VALUES (?, ?, ?, 'RUNNING', DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 48 HOUR))";
        try (PreparedStatement pstmt = conn.prepareStatement(insertAuction)) {
            pstmt.setInt(1, itemId);
            pstmt.setLong(2, 98000000L);
            pstmt.setLong(3, 128000000L);
            pstmt.executeUpdate();
        }
        logger.info(">>> Đã thêm phiên RUNNING mua đứt Leica Q3, còn hạn 48 giờ.");
    }

    // --- SEED MACBOOK PRO M3 MAX ---
    private static void seedPhienMuaDutMacBook(Connection conn) throws SQLException {
        int sellerId = laySellerId(conn);
        if (sellerId == -1) {
            return;
        }

        int itemId = layHoacTaoMacBook(conn, sellerId);
        if (daCoPhienChoItem(conn, itemId)) {
            logger.info(">>> Phiên mẫu MacBook Pro đã tồn tại. Không tạo bản trùng.");
            return;
        }

        String insertAuction = "INSERT INTO auctions " +
                "(item_id, current_price, buy_now_price, status, start_time, end_time) " +
                "VALUES (?, ?, ?, 'RUNNING', DATE_SUB(NOW(), INTERVAL 5 MINUTE), DATE_ADD(NOW(), INTERVAL 72 HOUR))";
        try (PreparedStatement pstmt = conn.prepareStatement(insertAuction)) {
            pstmt.setInt(1, itemId);
            pstmt.setLong(2, 115000000L); // Giá hiện tại (khởi điểm)
            pstmt.setLong(3, 150000000L); // Giá mua đứt
            pstmt.executeUpdate();
        }
        logger.info(">>> Đã thêm phiên RUNNING mua đứt MacBook Pro M3 Max, còn hạn 72 giờ.");
    }

    private static int laySellerId(Connection conn) throws SQLException {
        String sql = "SELECT id FROM users WHERE role = 'SELLER' ORDER BY id LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next() ? rs.getInt("id") : -1;
        }
    }

    private static int layHoacTaoLeicaQ3(Connection conn, int sellerId) throws SQLException {
        String itemName = "Leica Q3 Full Frame 60MP";
        String selectItem = "SELECT id FROM items WHERE seller_id = ? AND name = ? ORDER BY id LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(selectItem)) {
            pstmt.setInt(1, sellerId);
            pstmt.setString(2, itemName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String insertItem = "INSERT INTO items " +
                "(seller_id, name, description, category, starting_price, bid_increment, image_url) " +
                "VALUES (?, ?, ?, 'ELECTRONICS', ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertItem, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, sellerId);
            pstmt.setString(2, itemName);
            pstmt.setString(3, "Máy ảnh compact full-frame cao cấp, cảm biến 60MP, thiết kế bạc đen sang trọng.");
            pstmt.setLong(4, 98000000L);
            pstmt.setLong(5, 1000000L);
            pstmt.setString(6, "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800");
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Không lấy được id item Leica Q3 vừa tạo.");
    }

    private static int layHoacTaoMacBook(Connection conn, int sellerId) throws SQLException {
        String itemName = "MacBook Pro 16-inch M3 Max";
        String selectItem = "SELECT id FROM items WHERE seller_id = ? AND name = ? ORDER BY id LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(selectItem)) {
            pstmt.setInt(1, sellerId);
            pstmt.setString(2, itemName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String insertItem = "INSERT INTO items " +
                "(seller_id, name, description, category, starting_price, bid_increment, image_url) " +
                "VALUES (?, ?, ?, 'ELECTRONICS', ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertItem, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, sellerId);
            pstmt.setString(2, itemName);
            pstmt.setString(3, "Siêu phẩm laptop Apple, chip M3 Max 40-core GPU, RAM 128GB, SSD 4TB. Hoàn hảo cho đồ họa 3D và AI.");
            pstmt.setLong(4, 115000000L);
            pstmt.setLong(5, 2500000L);
            // Ảnh Unsplash MacBook chất lượng cao, khung hình đẹp
            pstmt.setString(6, "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&q=80");
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Không lấy được id item MacBook vừa tạo.");
    }

    private static boolean daCoPhienChoItem(Connection conn, int itemId) throws SQLException {
        String sql = "SELECT 1 FROM auctions WHERE item_id = ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}