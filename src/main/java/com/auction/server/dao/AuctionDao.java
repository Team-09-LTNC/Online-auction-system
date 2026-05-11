package com.auction.server.dao;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.item.*;
import com.auction.server.db.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tầng quản lý truy cập dữ liệu (DAO) cho các phiên đấu giá.
 */
public class AuctionDao {

    /**
     * Đa hình (Polymorphism): Khởi tạo đúng loại Item dựa vào category.
     */
    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        String loai = rs.getString("category").toUpperCase();
        Item item;
        switch (loai) {
            case "ELECTRONICS": item = new Electronics(); break;
            case "ART":         item = new Art(); break;
            case "VEHICLE":     item = new Vehicle(); break;
            default:            item = new OtherItem(); break;
        }

        item.setId(rs.getInt("item_id"));
        item.setName(rs.getString("name"));
        item.setStartingPrice(rs.getLong("starting_price"));
        item.setBidIncrement(rs.getLong("bid_increment"));
        item.setSellerId(rs.getInt("seller_id"));
        item.setCategory(loai);

        Auction phien = new Auction(item);
        phien.setId(rs.getInt("id"));
        phien.setStatus(AuctionStatus.valueOf(rs.getString("status").toUpperCase()));
        phien.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        phien.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        phien.setCurrentPrice(rs.getLong("current_price"));

        return phien;
    }

    public List<Auction> layDanhSachPhienDangChay() {
        return thucThiTruyVanDanhSach("SELECT a.*, i.name, i.category, i.starting_price, i.bid_increment, i.seller_id " +
                "FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.status = 'RUNNING'");
    }

    public List<Auction> layDanhSachPhienChoMo() {
        return thucThiTruyVanDanhSach("SELECT a.*, i.name, i.category, i.starting_price, i.bid_increment, i.seller_id " +
                "FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.status = 'OPEN'");
    }

    private List<Auction> thucThiTruyVanDanhSach(String sql) {
        List<Auction> danhSach = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Auction phien = mapResultSetToAuction(rs);
                if (phien != null) danhSach.add(phien);
            }
        } catch (SQLException e) { System.err.println("Lỗi truy vấn danh sách: " + e.getMessage()); }
        return danhSach;
    }

    /**
     * Đảm bảo Cập nhật giá và Lưu lịch sử diễn ra đồng thời. Nếu 1 bước lỗi, Rollback toàn bộ.
     */
    public boolean thucHienGiaoDichDatGia(int idPhien, BidTransaction tx) {
        String sqlUpdate = "UPDATE auctions SET current_price = ?, highest_bidder_id = ? " +
                "WHERE id = ? AND current_price < ? AND status = 'RUNNING'";
        String sqlInsert = "INSERT INTO bid_history (auction_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // 1. Bắt đầu Transaction

            // 2. Cập nhật bảng Auctions (Có kiểm tra điều kiện giá để chống Race Condition)
            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                psUpdate.setLong(1, tx.getBidAmount());
                psUpdate.setInt(2, tx.getBidder().getId());
                psUpdate.setInt(3, idPhien);
                psUpdate.setLong(4, tx.getBidAmount());
                if (psUpdate.executeUpdate() == 0) {
                    conn.rollback(); // Giá đã bị người khác đẩy lên trước, hủy giao dịch
                    return false;
                }
            }

            // 3. Lưu lịch sử đặt giá
            try (PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {
                psInsert.setInt(1, idPhien);
                psInsert.setInt(2, tx.getBidder().getId());
                psInsert.setLong(3, tx.getBidAmount());
                psInsert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                psInsert.executeUpdate();
            }

            conn.commit(); // 4. Hoàn tất giao dịch thành công
            return true;

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("Lỗi Transaction Đặt Giá: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public boolean capNhatTrangThai(int idPhien, String trangThaiMoi) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, trangThaiMoi.toUpperCase());
            pstmt.setInt(2, idPhien);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean capNhatThoiGianKetThuc(int idPhien, LocalDateTime thoiGianMoi) {
        String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(thoiGianMoi));
            pstmt.setInt(2, idPhien);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
}