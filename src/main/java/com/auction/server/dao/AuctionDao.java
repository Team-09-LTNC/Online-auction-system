package com.auction.server.dao;

import com.auction.common.model.AuctionStatus;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.item.*;
import com.auction.server.db.DatabaseConnection;
import com.auction.server.manager.ProductManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//Các cột cần có trong Database: ( bảng auctions )
//  id (INT, Primary Key, Auto Increment): ID duy nhất của phiên
//  item_id (INT): ID của sản phẩm (Khóa ngoại liên kết với bảng items)
//  current_price (BIGINT): Giá hiện tại
//  highest_bidder_id (INT, Nullable): ID của người đang trả giá cao nhất (Liên kết với bảng users)
//  start_time (DATETIME): Thời gian bắt đầu
//  end_time (DATETIME): Thời gian kết thúc
//  status (VARCHAR): Trạng thái  (OPEN, RUNNING, FINISHED, PAID, CANCELED)


/**
 * Tầng truy cập dữ liệu (DAO) cho các phiên đấu giá
 */
public class AuctionDao {

    /**
     * Lấy danh sách các phiên đang RUNNING để nạp vào bộ nhớ khi khởi động Server.
     */
    public List<Auction> layDanhSachPhienDangChay() {
        List<Auction> danhSach = new ArrayList<>();
        // Truy vấn kết hợp bảng auctions và items
        String sql = "SELECT a.*, i.name, i.category, i.starting_price, i.bid_increment, i.seller_id " +
                "FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.status = 'RUNNING'";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Sử dụng Factory Pattern để tạo đối tượng Item tương ứng
                Item item = ProductManager.getInstance().taoSanPham(rs.getString("category"), null);
                if (item != null) {
                    item.setId(rs.getInt("item_id"));
                    item.setName(rs.getString("name"));
                    item.setStartingPrice(rs.getLong("starting_price"));
                    item.setBidIncrement(rs.getLong("bid_increment"));
                    item.setSellerId(rs.getInt("seller_id"));

                    Auction phien = new Auction(item);
                    phien.setId(rs.getInt("id"));
                    phien.setStatus(AuctionStatus.RUNNING);
                    phien.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                    danhSach.add(phien);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi truy vấn danh sách phiên: " + e.getMessage());
        }
        return danhSach;
    }

    /**
     * Cập nhật giá mới vào DB. Sử dụng cơ chế kiểm tra giá cũ để tránh Lost Update
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
        } catch (SQLException e) { return false; }
    }

    /**
     * Cập nhật thời gian kết thúc (Dùng cho thuật toán Anti-sniping)[cite: 89, 90].
     */
    public boolean capNhatThoiGianKetThuc(int idPhien, LocalDateTime thoiGianMoi) {
        String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(thoiGianMoi));
            pstmt.setInt(2, idPhien);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    /**
     * Chuyển đổi trạng thái phiên (ví dụ: RUNNING -> FINISHED)[cite: 55].
     */
    public boolean capNhatTrangThai(int idPhien, String trangThaiMoi) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, trangThaiMoi.toUpperCase());
            pstmt.setInt(2, idPhien);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    /**
     * Tìm các phiên đã quá giờ nhưng chưa được đóng.
     */
    public List<Integer> layDanhSachPhienHetHan() {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT id FROM auctions WHERE end_time <= NOW() AND status = 'RUNNING'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getInt("id"));
        } catch (SQLException e) { }
        return list;
    }
}