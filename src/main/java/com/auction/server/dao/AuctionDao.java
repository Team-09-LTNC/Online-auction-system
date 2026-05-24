package com.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.bid.AutoBidConfig;
import com.auction.common.model.user.Bidder;

import com.auction.server.db.DatabaseConnection;

/**
 * Tầng quản lý truy cập dữ liệu (DAO) cho các phiên đấu giá.
 */
public class AuctionDao {
    private static final Logger logger = LoggerFactory.getLogger(AuctionDao.class);

    public static class AuctionNotificationTargets {
        public final int auctionId;
        public final int sellerId;
        public final Integer winnerId;
        public final String itemName;
        public final String winnerName;

        public AuctionNotificationTargets(
                int auctionId,
                int sellerId,
                Integer winnerId,
                String itemName,
                String winnerName
        ) {
            this.auctionId = auctionId;
            this.sellerId = sellerId;
            this.winnerId = winnerId;
            this.itemName = itemName;
            this.winnerName = winnerName;
        }
    }

    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        return AuctionRowMapper.mapResultSetToAuction(rs, logger);
    }

    public void capNhatTrangThaiTheoThoiGian() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "UPDATE auctions "
                            + "SET status = 'RUNNING' "
                            + "WHERE status = 'OPEN' "
                            + "AND start_time <= NOW() "
                            + "AND end_time > NOW()");
            stmt.executeUpdate(
                    "UPDATE auctions "
                            + "SET status = CASE "
                            + "    WHEN highest_bidder_id IS NULL THEN 'CANCELED' "
                            + "    ELSE 'FINISHED' "
                            + "END "
                            + "WHERE status IN ('OPEN', 'RUNNING') "
                            + "AND end_time <= NOW()");
        } catch (SQLException e) {
            logger.error("Lỗi cập nhật trạng thái phiên theo thời gian: ", e);
        }
    }

    public List<Auction> layDanhSachPhienDangChay() {
        capNhatTrangThaiTheoThoiGian();
        return thucThiTruyVanDanhSach(
                "SELECT a.*, i.name, i.description, i.category, i.starting_price, i.bid_increment, i.seller_id, i.image_url "
                        +
                        "FROM auctions a JOIN items i ON a.item_id = i.id "
                        + "WHERE a.status = 'RUNNING' AND a.start_time <= NOW() AND a.end_time > NOW()");
    }

    public List<Auction> layDanhSachPhienChoMo() {
        capNhatTrangThaiTheoThoiGian();
        return thucThiTruyVanDanhSach(
                "SELECT a.*, i.name, i.description, i.category, i.starting_price, i.bid_increment, i.seller_id, i.image_url "
                        +
                        "FROM auctions a JOIN items i ON a.item_id = i.id "
                        + "WHERE a.status = 'OPEN' AND a.start_time > NOW()");
    }

    public List<Auction> layDanhSachTatCaPhien() {
        capNhatTrangThaiTheoThoiGian();
        return thucThiTruyVanDanhSach(
                "SELECT a.*, i.name, i.description, i.category, i.starting_price, i.bid_increment, i.seller_id, i.image_url "
                        +
                        "FROM auctions a JOIN items i ON a.item_id = i.id ORDER BY a.start_time DESC, a.id DESC");
    }

    public List<Auction> laySauPhienDangChayNhieuBidNhat() {
        capNhatTrangThaiTheoThoiGian();
        return thucThiTruyVanDanhSach(
                "SELECT a.*, i.name, i.description, i.category, i.starting_price, i.bid_increment, i.seller_id, i.image_url, "
                        +
                        "COUNT(b.id) AS bid_count "
                        +
                        "FROM auctions a "
                        +
                        "JOIN items i ON a.item_id = i.id "
                        +
                        "LEFT JOIN bid_history b ON a.id = b.auction_id "
                        +
                        "WHERE a.status = 'RUNNING' AND a.start_time <= NOW() AND a.end_time > NOW() "
                        +
                        "GROUP BY a.id, i.id "
                        +
                        "ORDER BY bid_count DESC, a.id DESC "
                        +
                        "LIMIT 6");
    }

    // Lấy danh sách các phiên mà User đã tham gia đặt giá (hoặc là người bán)
    public List<Auction> layDanhSachPhienThamGia(int userId, String role) {
        if ("SELLER".equals(role)) {
            return layDanhSachPhienCuaSeller(userId);
        }

        return layDanhSachPhienBidderDaThamGia(userId);
    }

    private List<Auction> layDanhSachPhienCuaSeller(int sellerId) {
        String sql = "SELECT a.*, i.name, i.description, i.category, i.starting_price, "
                + "i.bid_increment, i.seller_id, i.image_url "
                + "FROM auctions a "
                + "JOIN items i ON a.item_id = i.id "
                + "WHERE i.seller_id = ? "
                + "ORDER BY a.start_time DESC, a.id DESC";
        return thucThiTruyVanDanhSach(sql, sellerId);
    }

    private List<Auction> layDanhSachPhienBidderDaThamGia(int bidderId) {
        String sql = "SELECT a.*, i.name, i.description, i.category, i.starting_price, "
                + "i.bid_increment, i.seller_id, i.image_url "
                + "FROM auctions a "
                + "JOIN items i ON a.item_id = i.id "
                + "JOIN ("
                + "  SELECT auction_id, MAX(bid_time) AS latest_bid_time "
                + "  FROM bid_history "
                + "  WHERE bidder_id = ? "
                + "  GROUP BY auction_id"
                + ") joined ON joined.auction_id = a.id "
                + "ORDER BY joined.latest_bid_time DESC, a.id DESC";
        return thucThiTruyVanDanhSach(sql, bidderId);
    }

    private List<Auction> thucThiTruyVanDanhSach(String sql) {
        List<Auction> danhSach = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Auction phien = mapResultSetToAuction(rs);
                if (phien != null)
                    danhSach.add(phien);
            }
        } catch (SQLException e) {
            logger.error("Lỗi truy vấn danh sách: ", e);
        }
        return danhSach;
    }

    private List<Auction> thucThiTruyVanDanhSach(String sql, int param) {
        List<Auction> danhSach = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, param);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Auction phien = mapResultSetToAuction(rs);
                    if (phien != null)
                        danhSach.add(phien);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi truy vấn danh sách có tham số: ", e);
        }
        return danhSach;
    }

    public int demPhienDangChay() {
        capNhatTrangThaiTheoThoiGian();
        return demTheoSql(
                "SELECT COUNT(*) FROM auctions WHERE status = 'RUNNING' AND start_time <= NOW() AND end_time > NOW()");
    }

    public int demPhienSapKetThuc() {
        capNhatTrangThaiTheoThoiGian();
        return demTheoSql(
                "SELECT COUNT(*) FROM auctions "
                        +
                        "WHERE status = 'RUNNING' AND start_time <= NOW() "
                        +
                        "AND end_time > NOW() AND end_time <= DATE_ADD(NOW(), INTERVAL 1 HOUR)");
    }

    public int demPhienBidderDaThamGia(int bidderId) {
        String sql = "SELECT COUNT(*) "
                + "FROM ("
                + "  SELECT DISTINCT b.auction_id "
                + "  FROM bid_history b "
                + "  JOIN auctions a ON a.id = b.auction_id "
                + "  WHERE b.bidder_id = ?"
                + ") joined";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bidderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            logger.error("Lỗi đếm phiên bidder đã tham gia: ", e);
            return 0;
        }
    }

    public int demPhienBidderDangThamGia(int bidderId) {
        capNhatTrangThaiTheoThoiGian();
        String sql = "SELECT COUNT(DISTINCT b.auction_id) "
                + "FROM bid_history b "
                + "JOIN auctions a ON a.id = b.auction_id "
                + "WHERE b.bidder_id = ? "
                + "AND a.status IN ('OPEN', 'RUNNING') "
                + "AND a.end_time > NOW()";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bidderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            logger.error("Lỗi đếm phiên bidder đang tham gia: ", e);
            return 0;
        }
    }

    private int demTheoSql(String sql) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            logger.error("Lỗi truy vấn số lượng phiên đấu giá: ", e);
            return 0;
        }
    }

    public boolean thucHienGiaoDichDatGia(int idPhien, BidTransaction tx) {
        String sqlUpdate = "UPDATE auctions SET current_price = ?, highest_bidder_id = ? " +
                "WHERE id = ? AND current_price < ? AND status = 'RUNNING' AND end_time > NOW()";
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
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean capNhatThoiGianKetThuc(int idPhien, LocalDateTime thoiGianMoi) {
        String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(thoiGianMoi));
            pstmt.setInt(2, idPhien);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean taoPhienDauGia(int itemId, long startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        return taoPhienDauGia(itemId, startingPrice, startTime, endTime, null, false);
    }

    public boolean taoPhienDauGia(
            int itemId,
            long startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long buyNowPrice
    ) {
        return taoPhienDauGia(itemId, startingPrice, startTime, endTime, buyNowPrice, false);
    }

    public boolean taoPhienDauGia(
            int itemId,
            long startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long buyNowPrice,
            boolean antiSnipingEnabled
    ) {
        String sql = "INSERT INTO auctions "
                + "(item_id, current_price, buy_now_price, anti_sniping_enabled, status, start_time, end_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            pstmt.setLong(2, startingPrice);
            if (buyNowPrice != null && buyNowPrice > 0) {
                pstmt.setLong(3, buyNowPrice);
            } else {
                pstmt.setNull(3, java.sql.Types.BIGINT);
            }

            pstmt.setBoolean(4, antiSnipingEnabled);

            String status = startTime.isAfter(LocalDateTime.now().plusSeconds(1)) ? "OPEN" : "RUNNING";
            pstmt.setString(5, status);

            pstmt.setTimestamp(6, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(7, Timestamp.valueOf(endTime));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi tạo phiên đấu giá: ", e);
            return false;
        }
    }

    public Auction layPhienTheoId(int idPhien) {
        capNhatTrangThaiTheoThoiGian();
        String sql = "SELECT a.*, i.name, i.description, i.category, i.starting_price, i.bid_increment, i.seller_id, i.image_url "
                + "FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.id = ?";
        List<Auction> danhSach = thucThiTruyVanDanhSach(sql, idPhien);
        return danhSach.isEmpty() ? null : danhSach.get(0);
    }

    public Auction layPhienTheoItemId(int itemId) {
        capNhatTrangThaiTheoThoiGian();
        String sql = "SELECT a.*, i.name, i.description, i.category, i.starting_price, "
                + "i.bid_increment, i.seller_id, i.image_url "
                + "FROM auctions a JOIN items i ON a.item_id = i.id "
                + "WHERE i.id = ? ORDER BY a.id DESC LIMIT 1";
        List<Auction> danhSach = thucThiTruyVanDanhSach(sql, itemId);
        return danhSach.isEmpty() ? null : danhSach.get(0);
    }

    public AuctionNotificationTargets layNguoiNhanThongBaoKetThuc(int auctionId) {
        String sql = "SELECT a.id, a.highest_bidder_id, i.seller_id, i.name, u.full_name "
                + "FROM auctions a "
                + "JOIN items i ON i.id = a.item_id "
                + "LEFT JOIN users u ON u.id = a.highest_bidder_id "
                + "WHERE a.id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                int rawWinnerId = rs.getInt("highest_bidder_id");
                Integer winnerId = rs.wasNull() ? null : rawWinnerId;
                return new AuctionNotificationTargets(
                        rs.getInt("id"),
                        rs.getInt("seller_id"),
                        winnerId,
                        rs.getString("name"),
                        rs.getString("full_name")
                );
            }
        } catch (SQLException e) {
            logger.error("Không lấy được người nhận thông báo kết thúc phiên {}.", auctionId, e);
            return null;
        }
    }

    public List<Auction> timKiemVaLocPhienDauGia(String keyword, String status) {
        capNhatTrangThaiTheoThoiGian();
        StringBuilder sql = new StringBuilder(
                "SELECT a.*, i.name, i.description, i.category, i.starting_price, i.bid_increment, i.seller_id, i.image_url "
                        +
                        "FROM auctions a JOIN items i ON a.item_id = i.id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND i.name LIKE ? ");
            params.add("%" + keyword.trim() + "%");
        }

        if (status != null && !status.equalsIgnoreCase("ALL") && !status.equalsIgnoreCase("Tất cả")) {
            sql.append("AND a.status = ? ");
            params.add(status.toUpperCase());
        }

        List<Auction> danhSach = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int j = 0; j < params.size(); j++) {
                pstmt.setObject(j + 1, params.get(j));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Auction phien = mapResultSetToAuction(rs);
                    if (phien != null)
                        danhSach.add(phien);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi timKiemVaLocPhienDauGia: ", e);
        }
        return danhSach;
    }

    /**
     * Lưu hoặc cập nhật cấu hình Auto-Bid của người dùng (Dùng cơ chế UPSERT của MySQL)
     */
    public boolean luuHoacCapNhatAutoBid(int auctionId, int bidderId, long maxAutoBid) {
        String sql = "INSERT INTO auto_bid_settings (auction_id, bidder_id, max_auto_bid) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE max_auto_bid = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setInt(1, auctionId);
            pstm.setInt(2, bidderId);
            pstm.setLong(3, maxAutoBid);
            pstm.setLong(4, maxAutoBid); 
            return pstm.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi lưu/cập nhật Auto-Bid: ", e);
            return false;
        }
    }

    /**
     * Lấy giá trần Auto-Bid của 1 user cụ thể trong 1 phiên (để Client gọi hiển thị lại lên UI)
     */
    public long layGiaTranAutoBid(int auctionId, int bidderId) {
        String sql = "SELECT max_auto_bid FROM auto_bid_settings WHERE auction_id = ? AND bidder_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setInt(1, auctionId);
            pstm.setInt(2, bidderId);
            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("max_auto_bid");
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi lấy giá trần Auto-Bid: ", e);
        }
        return -1; // Trả về -1 nếu user chưa cài đặt auto-bid
    }

    /**
     * Lấy TOÀN BỘ cấu hình Bot của 1 phiên đấu giá
     */
    public List<AutoBidConfig> layDanhSachAutoBidCuaPhien(int auctionId) {
        List<AutoBidConfig> dsBot = new ArrayList<>();
        String sql = "SELECT a.bidder_id, a.max_auto_bid, u.username, u.full_name " +
                "FROM auto_bid_settings a " +
                "JOIN users u ON a.bidder_id = u.id " +
                "WHERE a.auction_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setInt(1, auctionId);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    // Sử dụng đúng constructor của Bidder
                    Bidder u = new Bidder(
                            rs.getString("username"),
                            "",
                            rs.getString("full_name")
                    );
                    u.setId(rs.getInt("bidder_id")); // Kế thừa từ class Entity

                    dsBot.add(new AutoBidConfig(u, rs.getLong("max_auto_bid")));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi lấy danh sách Bot của phiên: ", e);
        }
        return dsBot;
    }
}
