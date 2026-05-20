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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tầng quản lý truy cập dữ liệu (DAO) cho các phiên đấu giá.
 */
public class AuctionDao {
    private static final Logger logger = LoggerFactory.getLogger(AuctionDao.class);

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

        // ---> CẬP NHẬT QUAN TRỌNG: ĐỌC MÔ TẢ SẢN PHẨM TỪ DATABASE <---
        try { item.setDescription(rs.getString("description")); } catch (Exception ignored) {}

        item.setStartingPrice(rs.getLong("starting_price"));
        item.setBidIncrement(rs.getLong("bid_increment"));
        item.setSellerId(rs.getInt("seller_id"));
        item.setCategory(loai);

        // Thêm an toàn khi load image_url (vì có thể query JOIN bị trùng tên cột, nên cần cẩn thận)
        try { item.setImageUrl(rs.getString("image_url")); } catch (Exception ignored) {}

        Auction phien = new Auction(item);
        phien.setId(rs.getInt("id"));
        phien.setStatus(AuctionStatus.valueOf(rs.getString("status").toUpperCase()));
        phien.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        phien.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        phien.setCurrentPrice(rs.getLong("current_price"));

        // ---> CẬP NHẬT: ĐỌC DỮ LIỆU GIÁ MUA ĐỨT TỪ DATABASE <---
        long buyNowPrice = rs.getLong("buy_now_price");
        if (!rs.wasNull()) {
            phien.setBuyNowPrice(buyNowPrice);
        }

        return phien;
    }

    public List<Auction> layDanhSachPhienDangChay() {
        // Đã bổ sung i.description vào câu lệnh SQL SELECT
        return thucThiTruyVanDanhSach("SELECT a.*, i.name, i.description, i.category, i.starting_price, i.bid_increment, i.seller_id, i.image_url " +
                "FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.status = 'RUNNING' OR a.status = 'OPEN'");
    }

    public List<Auction> layDanhSachPhienChoMo() {
        // Đã bổ sung i.description vào câu lệnh SQL SELECT
        return thucThiTruyVanDanhSach("SELECT a.*, i.name, i.description, i.category, i.starting_price, i.bid_increment, i.seller_id, i.image_url " +
                "FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.status = 'OPEN'");
    }

    // Lấy danh sách các phiên mà User đã tham gia đặt giá (hoặc là người bán)
    public List<Auction> layDanhSachPhienThamGia(int userId, String role) {
        String sql;
        if ("SELLER".equals(role)) {
            sql = "SELECT a.*, i.name, i.description, i.category, i.starting_price, i.bid_increment, i.seller_id, i.image_url " +
                    "FROM auctions a JOIN items i ON a.item_id = i.id WHERE i.seller_id = " + userId;
        } else {
            sql = "SELECT DISTINCT a.*, i.name, i.description, i.category, i.starting_price, i.bid_increment, i.seller_id, i.image_url " +
                    "FROM auctions a JOIN items i ON a.item_id = i.id " +
                    "JOIN bid_history b ON a.id = b.auction_id WHERE b.bidder_id = " + userId;
        }
        return thucThiTruyVanDanhSach(sql);
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
        } catch (SQLException e) { logger.error("Lỗi truy vấn danh sách: ", e); }
        return danhSach;
    }

    public boolean thucHienGiaoDichDatGia(int idPhien, BidTransaction tx) {
        String sqlUpdate = "UPDATE auctions SET current_price = ?, highest_bidder_id = ? " +
                "WHERE id = ? AND current_price < ? AND status = 'RUNNING'";
        String sqlInsert = "INSERT INTO bid_history (auction_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                psUpdate.setLong(1, tx.getBidAmount());
                psUpdate.setInt(2, tx.getBidder().getId());
                psUpdate.setInt(3, idPhien);
                psUpdate.setLong(4, tx.getBidAmount());
                if (psUpdate.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            try (PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {
                psInsert.setInt(1, idPhien);
                psInsert.setInt(2, tx.getBidder().getId());
                psInsert.setLong(3, tx.getBidAmount());
                psInsert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                psInsert.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Lỗi khi rollback transaction đặt giá: ", ex);
                }
            }
            logger.error("Lỗi Transaction Đặt Giá: ", e);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Lỗi khi đóng kết nối hoặc trả auto-commit: ", e);
                }
            }
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

    // TẠO PHIÊN ĐẤU GIÁ MỚI
    public boolean taoPhienDauGia(int itemId, long startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "INSERT INTO auctions (item_id, current_price, status, start_time, end_time) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            pstmt.setLong(2, startingPrice);

            // Nếu thời gian bắt đầu nhỏ hơn hoặc bằng hiện tại -> RUNNING, ngược lại OPEN
            String status = startTime.isBefore(LocalDateTime.now().plusSeconds(1)) ? "RUNNING" : "OPEN";
            pstmt.setString(3, status);

            pstmt.setTimestamp(4, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(5, Timestamp.valueOf(endTime));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi tạo phiên đấu giá: ", e);
            return false;
        }
    }

    //Lấy chi tiết 1 phiên trực tiếp từ Database
    public Auction layPhienTheoId(int idPhien) {
        // Đã bổ sung i.description vào câu lệnh SQL SELECT trực tiếp
        String sql = "SELECT a.*, i.name, i.description, i.category, i.starting_price, i.bid_increment, i.seller_id, i.image_url " +
                "FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.id = " + idPhien;
        List<Auction> danhSach = thucThiTruyVanDanhSach(sql);
        return danhSach.isEmpty() ? null : danhSach.get(0);
    }
}