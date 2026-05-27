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

import com.auction.server.db.DatabaseConnection;

/**
 * Tầng quản lý truy cập dữ liệu (DAO) cho các phiên đấu giá.
 */
public class AuctionDao {
    private static final Logger logger = LoggerFactory.getLogger(AuctionDao.class);
    private final AutoBidDao autoBidDao = new AutoBidDao();
    private final AuctionQueryDao queryDao = new AuctionQueryDao();

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

    public LocalDateTime getDatabaseNow() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT CURRENT_TIMESTAMP")) {
            if (rs.next()) {
                return rs.getObject(1, LocalDateTime.class);
            }
        } catch (SQLException e) {
            logger.warn("Khong lay duoc thoi gian hien tai tu DB, dung thoi gian JVM.", e);
        }
        return LocalDateTime.now();
    }

    public void updateStatusByTime() {
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

    public List<Auction> getRunningAuctions() {
        updateStatusByTime();
        return queryDao.getRunningAuctions();
    }

    public List<Auction> getPendingAuctionSessions() {
        updateStatusByTime();
        return queryDao.getPendingAuctionSessions();
    }

    public List<Auction> getAllAuctionSessions() {
        updateStatusByTime();
        return queryDao.getAllAuctionSessions();
    }

    public List<Auction> getTopRunningAuctionsByBids() {
        updateStatusByTime();
        return queryDao.getTopRunningAuctionsByBids();
    }

    // Lấy danh sách các phiên mà User đã tham gia đặt giá (hoặc là người bán)
    public List<Auction> getJoinedAuctions(int userId, String role) {
        return queryDao.getJoinedAuctions(userId, role);
    }

    public int countRunningAuctions() {
        updateStatusByTime();
        return queryDao.countRunningAuctions();
    }

    public int countEndingSoonAuctions() {
        updateStatusByTime();
        return queryDao.countEndingSoonAuctions();
    }

    public int countJoinedAuctions(int bidderId) {
        return queryDao.countJoinedAuctions(bidderId);
    }

    public int countActiveJoinedAuctions(int bidderId) {
        updateStatusByTime();
        return queryDao.countActiveJoinedAuctions(bidderId);
    }

    public boolean executeBidTransaction(int idPhien, BidTransaction tx) {
        String sqlUpdate = "UPDATE auctions SET current_price = ?, highest_bidder_id = ? " +
                "WHERE id = ? AND status = 'RUNNING' AND end_time > NOW() " +
                "AND (current_price < ? OR (highest_bidder_id IS NULL AND current_price <= ?))";
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
                psUpdate.setLong(5, tx.getBidAmount());
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

    public boolean updateStatus(int idPhien, String trangThaiMoi) {
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

    public boolean updateStatusIfCurrent(int auctionId, String expectedStatus, String newStatus) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ? AND status = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.toUpperCase());
            pstmt.setInt(2, auctionId);
            pstmt.setString(3, expectedStatus.toUpperCase());
            return pstmt.executeUpdate() == 1;
        } catch (SQLException e) {
            logger.error("Không cập nhật được trạng thái phiên {} từ {} sang {}.",
                    auctionId, expectedStatus, newStatus, e);
            return false;
        }
    }

    public boolean updateEndTime(int idPhien, LocalDateTime thoiGianMoi) {
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

    public boolean createAuctionSession(int itemId, long startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        return createAuctionSession(itemId, startingPrice, startTime, endTime, null, false);
    }

    public boolean createAuctionSession(
            int itemId,
            long startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long buyNowPrice
    ) {
        return createAuctionSession(itemId, startingPrice, startTime, endTime, buyNowPrice, false);
    }

    public boolean createAuctionSession(
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

            // pstmt.setString(5,"PENDING");

            pstmt.setTimestamp(6, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(7, Timestamp.valueOf(endTime));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi tạo phiên đấu giá: ", e);
            return false;
        }
    }

    public Auction getAuctionById(int idPhien) {
        updateStatusByTime();
        return queryDao.getAuctionById(idPhien);
    }

    public Auction getAuctionByItemId(int itemId) {
        updateStatusByTime();
        return queryDao.getAuctionByItemId(itemId);
    }

    public AuctionNotificationTargets getAuctionEndNotificationTargets(int auctionId) {
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

    public List<AuctionNotificationTargets> getOverduePaymentTargets(LocalDateTime cutoffTime) {
        String sql = "SELECT a.id, a.highest_bidder_id, i.seller_id, i.name, u.full_name "
                + "FROM auctions a "
                + "JOIN items i ON i.id = a.item_id "
                + "LEFT JOIN users u ON u.id = a.highest_bidder_id "
                + "WHERE a.status = 'FINISHED' "
                + "AND a.highest_bidder_id IS NOT NULL "
                + "AND (a.end_time <= ? OR EXISTS ("
                + "  SELECT 1 FROM chat_messages cm "
                + "  WHERE cm.auction_id = a.id "
                + "  AND cm.recipient_id = a.highest_bidder_id "
                + "  AND cm.payment_required = TRUE "
                + "  AND cm.send_time <= ?"
                + "))";
        List<AuctionNotificationTargets> targets = queryOverduePaymentTargets(sql, cutoffTime, true);
        if (!targets.isEmpty()) {
            return targets;
        }

        String fallbackSql = "SELECT a.id, a.highest_bidder_id, i.seller_id, i.name, u.full_name "
                + "FROM auctions a "
                + "JOIN items i ON i.id = a.item_id "
                + "LEFT JOIN users u ON u.id = a.highest_bidder_id "
                + "WHERE a.status = 'FINISHED' "
                + "AND a.highest_bidder_id IS NOT NULL "
                + "AND a.end_time <= ?";
        return queryOverduePaymentTargets(fallbackSql, cutoffTime, false);
    }

    private List<AuctionNotificationTargets> queryOverduePaymentTargets(
            String sql,
            LocalDateTime cutoffTime,
            boolean hasNotificationCutoff
    ) {
        List<AuctionNotificationTargets> targets = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            Timestamp cutoff = Timestamp.valueOf(cutoffTime);
            pstmt.setTimestamp(1, cutoff);
            if (hasNotificationCutoff) {
                pstmt.setTimestamp(2, cutoff);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    targets.add(new AuctionNotificationTargets(
                            rs.getInt("id"),
                            rs.getInt("seller_id"),
                            rs.getInt("highest_bidder_id"),
                            rs.getString("name"),
                            rs.getString("full_name")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.warn("Không truy vấn được danh sách phiên quá hạn thanh toán.", e);
        }
        return targets;
    }

    public List<Auction> searchAndFilterAuctions(String keyword, String status) {
        updateStatusByTime();
        return queryDao.searchAndFilterAuctions(keyword, status);
    }
    public boolean saveOrUpdateAutoBid(int auctionId, int bidderId, long maxAutoBid, long bidStep) {
        return autoBidDao.saveOrUpdateAutoBid(auctionId, bidderId, maxAutoBid, bidStep);
    }

    public long getMaxAutoBid(int auctionId, int bidderId) {
        return autoBidDao.getMaxAutoBid(auctionId, bidderId);
    }

    public long getAutoBidStep(int auctionId, int bidderId) {
        return autoBidDao.getAutoBidStep(auctionId, bidderId);
    }

    public boolean removeAutoBid(int auctionId, int bidderId) {
        return autoBidDao.removeAutoBid(auctionId, bidderId);
    }

    public List<AutoBidConfig> getAuctionAutoBids(int auctionId) {
        return autoBidDao.getAuctionAutoBids(auctionId);
    }
}
