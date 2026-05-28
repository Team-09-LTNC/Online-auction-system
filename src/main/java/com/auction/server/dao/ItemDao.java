package com.auction.server.dao;

import com.auction.common.model.item.*;
import com.auction.server.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ItemDao: Chịu trách nhiệm tương tác với bảng 'items' trong cơ sở dữ liệu.
 * Lớp này thực hiện các thao tác CRUD và tìm kiếm sản phẩm.
 */
public class ItemDao {
    private static final Logger logger = LoggerFactory.getLogger(ItemDao.class);
    private static final AtomicBoolean checkedImageThumbColumn = new AtomicBoolean(false);

    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        Item item;
        String loai = rs.getString("category").trim().toUpperCase();


        switch (loai) {
            case "ELECTRONICS":
            case "ĐIỆN TỬ":
                item = new Electronics(); break;
            case "ART":
            case "NGHỆ THUẬT":
                item = new Art(); break;
            case "VEHICLE":
            case "PHƯƠNG TIỆN":
                item = new Vehicle(); break;
            case "OTHER":
            case "KHÁC":
                item = new OtherItem(); break;
            default:
                logger.warn("[ItemDao] Dữ liệu category không khớp chuẩn: '{}'. Tự động gán vào OtherItem.", loai);
                item = new OtherItem(); break;
        }

        item.setId(rs.getInt("id"));

        item.setSellerId(rs.getInt("seller_id"));

        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setStartingPrice(rs.getLong("starting_price"));
        item.setCategory(loai);
        item.setImageUrl(rs.getString("image_url"));
        try {
            item.setImageThumbUrl(rs.getString("image_thumb_url"));
        } catch (SQLException ignored) {
        }

        try {
            item.setStartTime(rs.getString("start_time"));
            item.setEndTime(rs.getString("end_time"));
        } catch (SQLException ignored) {
        }

        return item;
    }

    public int saveProduct(Item item) {
        ensureImageThumbColumn();
        String sql = "INSERT INTO items "
                + "(seller_id, name, description, category, starting_price, bid_increment, image_url, image_thumb_url) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, item.getSellerId());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setString(4, item.getCategory().trim().toUpperCase());
            pstmt.setLong(5, item.getStartingPrice());
            pstmt.setLong(6, item.getBidIncrement());
            pstmt.setString(7, item.getImageUrl() != null ? item.getImageUrl() : "");
            pstmt.setString(8, item.getImageThumbUrl() != null ? item.getImageThumbUrl() : "");

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi luuSanPham: ", e);
        }
        return -1;
    }

    public List<Item> getAllProducts() {
        ensureImageThumbColumn();
        List<Item> danhSach = new ArrayList<>();
        String sql = "SELECT * FROM items";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Item item = mapResultSetToItem(rs);
                if (item != null) danhSach.add(item);
            }
        } catch (SQLException e) {
            logger.error("Lỗi layTatCaSanPham: ", e);
        }
        return danhSach;
    }

    // Lấy danh sách sản phẩm do một người bán cụ thể đăng bán
    public List<Item> getProductsBySellerId(int sellerId) {
        ensureImageThumbColumn();
        List<Item> danhSach = new ArrayList<>();

        String sql = "SELECT i.*, a.start_time, a.end_time " +
                "FROM items i " +
                "LEFT JOIN auctions a ON i.id = a.item_id " +
                "WHERE i.seller_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Ánh xạ các thông tin cơ bản
                    Item item = mapResultSetToItem(rs);

                    // Ánh xạ thêm thời gian vào đối tượng Item
                    if (item != null) {
                        item.setStartTime(rs.getString("start_time"));
                        item.setEndTime(rs.getString("end_time"));
                        danhSach.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi laySanPhamTheoSellerId: ", e);
        }
        return danhSach;
    }

    public Item getProductById(int itemId) {
        ensureImageThumbColumn();
        String sql = "SELECT i.*, a.start_time, a.end_time "
                + "FROM items i "
                + "LEFT JOIN auctions a ON i.id = a.item_id "
                + "WHERE i.id = ? "
                + "ORDER BY a.id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToItem(rs);
            }
        } catch (SQLException e) {
            logger.error("Lỗi laySanPhamTheoId: ", e);
        }
        return null;
    }

    public List<Item> searchProductsByKeyword(String keyword) {
        ensureImageThumbColumn();
        List<Item> danhSach = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE name LIKE ? OR description LIKE ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Item item = mapResultSetToItem(rs);
                    if (item != null) danhSach.add(item);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi timKiemSanPham: ", e);
        }
        return danhSach;
    }

    public boolean deleteProduct(int itemId) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi xoaSanPham: ", e);
            return false;
        }
    }

    public boolean updateProduct(Item item) {
        ensureImageThumbColumn();
        String sql = "UPDATE items SET name = ?, description = ?, starting_price = ?, "
                + "category = ?, image_url = ?, image_thumb_url = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setLong(3, item.getStartingPrice());
            pstmt.setString(4, item.getCategory());
            pstmt.setString(5, item.getImageUrl());
            pstmt.setString(6, item.getImageThumbUrl());
            pstmt.setInt(7, item.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi updateSanPham: ", e);
            return false;
        }
    }

    private void ensureImageThumbColumn() {
        if (checkedImageThumbColumn.get()) {
            return;
        }

        synchronized (ItemDao.class) {
            if (checkedImageThumbColumn.get()) {
                return;
            }

            try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                try (ResultSet columns = metaData.getColumns(null, null, "items", "image_thumb_url")) {
                    if (columns.next()) {
                        checkedImageThumbColumn.set(true);
                        return;
                    }
                }

                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE items ADD COLUMN image_thumb_url VARCHAR(500) NULL");
                }
                checkedImageThumbColumn.set(true);
            } catch (Exception e) {
                logger.error("Cannot ensure image_thumb_url column.", e);
            }
        }
    }
}
