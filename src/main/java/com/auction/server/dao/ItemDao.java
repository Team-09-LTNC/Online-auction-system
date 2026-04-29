package com.auction.server.dao;

import com.auction.auction.AuctionStatus;
import com.auction.common.model.item.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO quản lý thao tác CRUD cho bảng items.
 *
 * LƯU Ý về ID:
 *   - Bảng items dùng id INT AUTO_INCREMENT (dbId).
 *   - Entity.id là UUID String (dùng nội bộ Java, gửi qua Socket).
 *   Hai loại ID này khác nhau — ItemRecord bọc cả hai để tránh nhầm lẫn.
 */
public class ItemDao {

    // ---------------------------------------------------------------------------
    // Inner class: bọc Item + dbId (INT) trả về cho caller khi cần JOIN với DB
    // ---------------------------------------------------------------------------
    public static class ItemRecord {
        public final int dbId;
        public final Item item;
        public final double currentPrice;
        public final AuctionStatus status;

        public ItemRecord(int dbId, Item item, double currentPrice, AuctionStatus status) {
            this.dbId = dbId;
            this.item = item;
            this.currentPrice = currentPrice;
            this.status = status;
        }
    }

    private final Connection conn;

    public ItemDao(Connection conn) {
        this.conn = conn;
    }

    // -------------------------------------------------------------------------
    // LƯU ITEM MỚI
    // Trả về dbId (INT) được DB gán, hoặc -1 nếu thất bại.
    // -------------------------------------------------------------------------
    public int saveItem(Item item, String sellerId) {
        String sql = "INSERT INTO items (name, description, category, starting_price, current_price, status, seller_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setString(3, item.getItemCategory());
            pstmt.setDouble(4, item.getStartingPrice());
            pstmt.setDouble(5, item.getStartingPrice()); // Giá hiện tại ban đầu = giá khởi điểm
            pstmt.setString(6, AuctionStatus.OPEN.name());
            pstmt.setString(7, sellerId);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi SQL tại ItemDao: " + e.getMessage());
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // TÌM THEO dbId (INT) — dùng khi JOIN với bảng bid_transaction
    // -------------------------------------------------------------------------
    public ItemRecord findByDbId(int dbId) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, dbId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi tìm item theo dbId: " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // LẤY TẤT CẢ ITEM
    // -------------------------------------------------------------------------
    public List<ItemRecord> findAll() {
        List<ItemRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM items ORDER BY id DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi lấy danh sách item: " + e.getMessage());
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // LẤY ITEM THEO TRẠNG THÁI (VD: OPEN, RUNNING)
    // -------------------------------------------------------------------------
    public List<ItemRecord> findByStatus(AuctionStatus status) {
        List<ItemRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE status = ? ORDER BY id DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi lọc item theo status: " + e.getMessage());
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // CẬP NHẬT GIÁ HIỆN TẠI VÀ TRẠNG THÁI
    // Gọi sau mỗi bid hợp lệ và khi phiên kết thúc.
    // -------------------------------------------------------------------------
    public boolean updatePriceAndStatus(int dbId, double currentPrice, AuctionStatus status) {
        String sql = "UPDATE items SET current_price = ?, status = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, currentPrice);
            pstmt.setString(2, status.name());
            pstmt.setInt(3, dbId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi cập nhật item: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // CẬP NHẬT THÔNG TIN SẢN PHẨM (Seller sửa name/description trước khi mở phiên)
    // Chỉ cho phép sửa khi status = OPEN (chưa có ai đấu giá).
    // -------------------------------------------------------------------------
    public boolean updateItemInfo(int dbId, String name, String description, double startingPrice) {
        String sql = "UPDATE items SET name = ?, description = ?, starting_price = ?, current_price = ? "
                + "WHERE id = ? AND status = 'OPEN'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setDouble(3, startingPrice);
            pstmt.setDouble(4, startingPrice); // reset current_price khi sửa giá khởi điểm
            pstmt.setInt(5, dbId);
            int rows = pstmt.executeUpdate();
            if (rows == 0) {
                System.err.println("⚠️ Không thể sửa item " + dbId + ": không tồn tại hoặc đã RUNNING.");
            }
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi cập nhật thông tin item: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // XÓA ITEM
    // Theo đề bài chỉ Seller/Admin được xóa, nên kiểm tra status ở tầng trên.
    // DAO chỉ thực thi xóa thuần túy.
    // -------------------------------------------------------------------------
    public boolean deleteById(int dbId) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, dbId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi xóa item: " + e.getMessage());
            return false;
        }
    }

    public void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println(">>> [INFO] Database connection đã được đóng.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi đóng connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    // PRIVATE HELPER — map ResultSet → ItemRecord
    // =========================================================================
    private ItemRecord mapRow(ResultSet rs) throws SQLException {
        int dbId            = rs.getInt("id");
        String name         = rs.getString("name");
        String description  = rs.getString("description");
        double startPrice   = rs.getDouble("starting_price");
        double currentPrice = rs.getDouble("current_price");
        String category     = rs.getString("category");
        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));

        Item item;
        switch (category) {
            case "ART":
                // Các trường đặc thù (artist, year, medium) chưa có cột riêng trong DB.
                // Để mở rộng sau: thêm cột hoặc bảng item_details.
                item = new Art(name, description, startPrice, "Unknown", 0, "Unknown");
                break;
            case "ELECTRONICS":
                item = new Electronics(name, description, startPrice, "Unknown", 0);
                break;
            case "VEHICLE":
                item = new Vehicle(name, description, startPrice, "Unknown", "Unknown", 0);
                break;
            default:
                throw new SQLException("Loại item không xác định: " + category);
        }

        return new ItemRecord(dbId, item, currentPrice, status);
    }
}
