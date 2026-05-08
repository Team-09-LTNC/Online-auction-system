package com.auction.server.dao;

import com.auction.server.db.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Nhiệm vụ: Xử lý các phiên đấu giá

// Các cột cần có trong Database: ( bảng auctions )
//  id (INT, Primary Key, Auto Increment): ID duy nhất của phiên
//  item_id (INT): ID của sản phẩm (Khóa ngoại liên kết với bảng items)
//  current_price (BIGINT): Giá hiện tại
//  highest_bidder_id (INT, Nullable): ID của người đang trả giá cao nhất (Liên kết với bảng users)
//  start_time (DATETIME): Thời gian bắt đầu
//  end_time (DATETIME): Thời gian kết thúc
//  status (VARCHAR): Trạng thái  (OPEN, RUNNING, FINISHED, PAID, CANCELED)

/**
 * Xử lý mọi thao tác tương tác cơ sở dữ liệu liên quan đến phiên đấu giá
 */
public class AuctionDao {

    /**
     * Tạo một phiên đấu giá mới
     */
    public boolean taoPhienMoi(int idSanPham, long giaKhoiDiem, Timestamp thoiGianBatDau, Timestamp thoiGianKetThuc) {
        String sql = "INSERT INTO auctions (item_id, current_price, start_time, end_time, status) VALUES (?, ?, ?, ?, 'RUNNING')";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idSanPham);
            pstmt.setLong(2, giaKhoiDiem);
            pstmt.setTimestamp(3, thoiGianBatDau);
            pstmt.setTimestamp(4, thoiGianKetThuc);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi taoPhienMoi: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật người dẫn đầu khi có lệnh đặt giá hợp lệ
     */
    public boolean capNhatGiaVaNguoiDanDau(int idPhien, long giaMoi, int idNguoiBid) {
        String sql = "UPDATE auctions SET current_price = ?, highest_bidder_id = ? " +
                "WHERE id = ? AND current_price < ? AND status = 'RUNNING'";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, giaMoi);
            pstmt.setInt(2, idNguoiBid);
            pstmt.setInt(3, idPhien);
            pstmt.setLong(4, giaMoi);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi capNhatGiaVaNguoiDanDau: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật thời gian kết thúc (Phục vụ thuật toán Anti-sniping)
     */
    public boolean capNhatThoiGianKetThuc(int idPhien, LocalDateTime thoiGianMoi) {
        String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(thoiGianMoi));
            pstmt.setInt(2, idPhien);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi capNhatThoiGianKetThuc: " + e.getMessage());
            return false;
        }
    }

    /**
     * Chuyển trạng thái của phiên (Từ RUNNING sang FINISHED)
     */
    public boolean capNhatTrangThai(int idPhien, String trangThaiMoi) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, trangThaiMoi.toUpperCase());
            pstmt.setInt(2, idPhien);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi capNhatTrangThai: " + e.getMessage());
            return false;
        }
    }

    /**
     * Quét và lấy danh sách ID các phiên đã hết thời gian nhưng chưa được đóng
     */
    public List<Integer> layDanhSachPhienHetHan() {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT id FROM auctions WHERE end_time <= NOW() AND status = 'RUNNING'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi layDanhSachPhienHetHan: " + e.getMessage());
        }
        return list;
    }
}