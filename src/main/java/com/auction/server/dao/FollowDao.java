package com.auction.server.dao;

import com.auction.server.db.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FollowDao {
    private static final Logger logger = LoggerFactory.getLogger(FollowDao.class);

    public FollowDao() {
        createFollowsTableIfNotExists();
    }

    private void createFollowsTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS follows (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id INT NOT NULL," +
                "  auction_id INT NOT NULL," +
                "  UNIQUE KEY unique_follow (user_id, auction_id)," +
                "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                "  FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE" +
                ")";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            logger.error("Không thể tạo bảng 'follows': ", e);
        }
    }

    public boolean follow(int userId, int auctionId) {
        String sql = "INSERT IGNORE INTO follows (user_id, auction_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi follow: ", e);
            return false;
        }
    }

    public boolean unfollow(int userId, int auctionId) {
        String sql = "DELETE FROM follows WHERE user_id = ? AND auction_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi unfollow: ", e);
            return false;
        }
    }

    public List<Integer> getFollowedAuctionIds(int userId) {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT auction_id FROM follows WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getInt("auction_id"));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy danh sách theo dõi: ", e);
        }
        return list;
    }

    public boolean isFollowing(int userId, int auctionId) {
        String sql = "SELECT 1 FROM follows WHERE user_id = ? AND auction_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Lỗi kiểm tra trạng thái theo dõi: ", e);
            return false;
        }
    }

    public int countFollowedAuctions(int userId) {
        String sql = "SELECT COUNT(*) FROM follows WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            logger.error("Lỗi đếm danh sách theo dõi: ", e);
            return 0;
        }
    }
}
