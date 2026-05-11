package com.auction.server.dao;

import com.auction.common.model.item.*;
import com.auction.server.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemDao: Chịu trách nhiệm tương tác với bảng 'items' trong Database.
 * Lớp này thực hiện các thao tác CRUD (Thêm, Đọc, Sửa, Xóa) và tìm kiếm sản phẩm.
 */
public class ItemDao {

    /**
     * PHƯƠNG THỨC HỖ TRỢ (Helper Method):
     * Chuyển đổi một dòng dữ liệu từ ResultSet (DB) thành đối tượng Item (Java).
     */
    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        // Lấy loại sản phẩm để khởi tạo đúng lớp con
        String loai = rs.getString("category").toUpperCase();
        Item item;

        // Factory logic: Dựa vào cột 'category' để tạo đối tượng tương ứng
        switch (loai) {
            case "ELECTRONICS": item = new Electronics(); break;
            case "ART":         item = new Art(); break;
            case "VEHICLE":     item = new Vehicle(); break;
            case "OTHER":       item = new OtherItem(); break;
            default:            return null; // Trả về null nếu loại không hợp lệ
        }

        // Đổ dữ liệu từ các cột trong Database vào các thuộc tính của đối tượng
        item.setId(rs.getInt("id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setStartingPrice(rs.getLong("starting_price"));
        item.setCategory(loai);
        item.setImageUrl(rs.getString("image_url"));

        return item;
    }

    /**
     * Lưu một sản phẩm mới vào cơ sở dữ liệu.
     * Sử dụng PreparedStatement để ngăn chặn SQL Injection.
     */
    public boolean luuSanPham(Item item) {
        String sql = "INSERT INTO items (seller_id, name, description, category, starting_price, image_url) VALUES (?, ?, ?, ?, ?, ?)";
        // Sử dụng try-with-resources để tự động đóng Connection và PreparedStatement
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, item.getSellerId());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setString(4, item.getCategory().toUpperCase());
            pstmt.setLong(5, item.getStartingPrice());
            pstmt.setString(6, item.getImageUrl());

            return pstmt.executeUpdate() > 0; // Trả về true nếu thêm thành công ít nhất 1 dòng
        } catch (SQLException e) {
            System.err.println("Lỗi luuSanPham: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy toàn bộ danh sách sản phẩm hiện có.
     */
    public List<Item> layTatCaSanPham() {
        List<Item> danhSach = new ArrayList<>();
        String sql = "SELECT * FROM items";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Gọi hàm mapResultSetToItem để chuyển dữ liệu dòng hiện tại thành đối tượng
                Item item = mapResultSetToItem(rs);
                if (item != null) {
                    danhSach.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi layTatCaSanPham: " + e.getMessage());
        }
        return danhSach;
    }

    /**
     * Tìm một sản phẩm cụ thể dựa trên mã ID.
     */
    public Item laySanPhamTheoId(int itemId) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToItem(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi laySanPhamTheoId: " + e.getMessage());
        }
        return null;
    }

    /**
     * Tìm kiếm sản phẩm theo từ khóa (trong tên hoặc mô tả).
     * Tương ứng với hành động PRODUCT_SEARCH trong ActionType.
     */
    public List<Item> searchItem(String keyword) {
        List<Item> danhSach = new ArrayList<>();
        // Sử dụng toán tử LIKE với ký tự % để tìm kiếm chuỗi con
        String sql = "SELECT * FROM items WHERE name LIKE ? OR description LIKE ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Cấu hình tham số tìm kiếm: %keyword%
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Item item = mapResultSetToItem(rs);
                    if (item != null) {
                        danhSach.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi searchItem: " + e.getMessage());
        }
        return danhSach;
    }

    /**
     * Xóa sản phẩm khỏi hệ thống dựa trên ID.
     */
    public boolean xoaSanPham(int itemId) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi xoaSanPham: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật thông tin mới cho một sản phẩm đã tồn tại.
     */
    public boolean updateSanPham(Item item) {
        String sql = "UPDATE items SET name = ?, description = ?, starting_price = ?, category = ?, image_url = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setLong(3, item.getStartingPrice());
            pstmt.setString(4, item.getCategory());
            pstmt.setString(5, item.getImageUrl());
            pstmt.setInt(6, item.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi updateSanPham: " + e.getMessage());
            return false;
        }
    }
}