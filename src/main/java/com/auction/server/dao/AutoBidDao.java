package com.auction.server.dao;

import com.auction.common.model.bid.AutoBidConfig;
import com.auction.common.model.user.Bidder;
import com.auction.server.db.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDao {
    private static final Logger logger = LoggerFactory.getLogger(AutoBidDao.class);

    public boolean saveOrUpdateAutoBid(int auctionId, int bidderId, long maxAutoBid, long bidStep) {
        String sql = "INSERT INTO auto_bid_settings (auction_id, bidder_id, max_auto_bid, bid_step) "
                + "VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE max_auto_bid = ?, bid_step = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setInt(1, auctionId);
            pstm.setInt(2, bidderId);
            pstm.setLong(3, maxAutoBid);
            pstm.setLong(4, bidStep);
            pstm.setLong(5, maxAutoBid);
            pstm.setLong(6, bidStep);
            return pstm.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi lưu/cập nhật Auto-Bid: ", e);
            return false;
        }
    }

    public long getMaxAutoBid(int auctionId, int bidderId) {
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
        return -1;
    }

    public long getAutoBidStep(int auctionId, int bidderId) {
        String sql = "SELECT bid_step FROM auto_bid_settings WHERE auction_id = ? AND bidder_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setInt(1, auctionId);
            pstm.setInt(2, bidderId);
            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("bid_step");
                }
            }
        } catch (SQLException e) {
            logger.error("Loi khi lay buoc gia Auto-Bid: ", e);
        }
        return -1;
    }

    public boolean removeAutoBid(int auctionId, int bidderId) {
        String sql = "DELETE FROM auto_bid_settings WHERE auction_id = ? AND bidder_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setInt(1, auctionId);
            pstm.setInt(2, bidderId);
            pstm.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Loi khi xoa Auto-Bid: ", e);
            return false;
        }
    }

    public List<AutoBidConfig> getAuctionAutoBids(int auctionId) {
        List<AutoBidConfig> autoBids = new ArrayList<>();
        String sql = "SELECT a.bidder_id, a.max_auto_bid, a.bid_step, a.register_time, "
                + "u.username, u.full_name "
                + "FROM auto_bid_settings a "
                + "JOIN users u ON a.bidder_id = u.id "
                + "WHERE a.auction_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setInt(1, auctionId);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    Bidder bidder = new Bidder(rs.getString("username"), "", rs.getString("full_name"));
                    bidder.setId(rs.getInt("bidder_id"));

                    Timestamp registerTime = rs.getTimestamp("register_time");
                    autoBids.add(new AutoBidConfig(
                            bidder,
                            rs.getLong("max_auto_bid"),
                            rs.getLong("bid_step"),
                            registerTime != null ? registerTime.toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi lấy danh sách Bot của phiên: ", e);
        }
        return autoBids;
    }
}
