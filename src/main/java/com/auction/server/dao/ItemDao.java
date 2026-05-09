package com.auction.server.dao;

import com.auction.common.model.item.*;
import com.auction.server.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Nhiệm vụ: Quản lý thông tin các sản phẩm đấu giá (CRUD)

// Các cột cần có trong Database: (bảng items)
//  id (INT, Primary Key, Auto Increment): ID duy nhất của sản phẩm
//  seller_id (INT): ID của người đăng bán (Liên kết với bảng users)
//  name (VARCHAR): Tên sản phẩm
//  description (TEXT): Mô tả chi tiết
//  category (VARCHAR): Loại sản phẩm (ELECTRONICS, ART, VEHICLE)
//  starting_price (BIGINT): Giá khởi điểm
//  bid_increment (BIGINT): khoảng tăng giá( seller tự set)
//  image_url (VARCHAR): Đường dẫn ảnh sản phẩm

public class ItemDao {

    /**
     * Thêm một sản phẩm mới vào hệ thống (thường dùng cho Seller)
     */
    public boolean luuSanPham(Item item) {
        String sql = "INSERT INTO items (seller_id, name, description, category, starting_price, image_url) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, item.getSellerId());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setString(4, item.getCategory().toUpperCase());
            pstmt.setLong(5, item.getStartingPrice());
            pstmt.setString(6, item.getImageUrl());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi luuSanPham: " + e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            System.err.println("Lỗi chuyển đổi sellerId: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy danh sách tất cả sản phẩm hiện có trong kho dữ liệu
     */
    public List<Item> layTatCaSanPham() {
        List<Item> danhSach = new ArrayList<>();
        String sql = "SELECT * FROM items";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String loai = rs.getString("category").toUpperCase();
                Item item;

                // Khởi tạo đúng lớp con dựa trên cột category (Tính đa hình)
                switch (loai) {
                    case "ELECTRONICS": item = new Electronics(); break;
                    case "ART":         item = new Art(); break;
                    case "VEHICLE":     item = new Vehicle(); break;
                    case "OTHER":       item = new OtherItem(); break;
                    default:            item = null; break;
                }

                if (item != null) {
                    item.setId(rs.getInt("id"));
                    item.setName(rs.getString("name"));
                    item.setDescription(rs.getString("description"));
                    item.setStartingPrice(rs.getLong("starting_price"));
                    item.setCategory(loai);
                    item.setImageUrl(rs.getString("image_url"));

                    danhSach.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi layTatCaSanPham: " + e.getMessage());
        }
        return danhSach;
    }

    /**
     * Lấy thông tin chi tiết của một sản phẩm theo ID
     */
    public Item laySanPhamTheoId(int itemId) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String loai = rs.getString("category").toUpperCase();
                    Item item;

                    switch (loai) {
                        case "ELECTRONICS": item = new Electronics(); break;
                        case "ART":         item = new Art(); break;
                        case "VEHICLE":     item = new Vehicle(); break;
                        case "OTHER":       item = new OtherItem(); break;
                        default:            item = null; break;
                    }

                    if (item != null) {
                        item.setId(rs.getInt("id"));
                        item.setName(rs.getString("name"));
                        item.setDescription(rs.getString("description"));
                        item.setStartingPrice(rs.getLong("starting_price"));
                        item.setCategory(loai);
                        item.setImageUrl(rs.getString("image_url"));
                    }
                    return item;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi laySanPhamTheoId: " + e.getMessage());
        }
        return null;
    }

    /**
     * Xóa sản phẩm khỏi danh sách ( dùng cho chức năng quản lý của Seller hoặc Admin)
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
    // Cập nhật sản phẩm
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
            return false;
        }
    }
}