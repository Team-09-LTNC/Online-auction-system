package com.auction.server.dao;

import com.auction.server.db.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class BidderPenaltyDao {
    private static final Logger logger = LoggerFactory.getLogger(BidderPenaltyDao.class);

    public static class SanctionResult {
        public final int violationCount;
        public final boolean permanentLock;
        public final LocalDateTime lockUntil;

        public SanctionResult(int violationCount, boolean permanentLock, LocalDateTime lockUntil) {
            this.violationCount = violationCount;
            this.permanentLock = permanentLock;
            this.lockUntil = lockUntil;
        }
    }

    public static class LockInfo {
        public final boolean locked;
        public final LocalDateTime lockUntil;

        public LockInfo(boolean locked, LocalDateTime lockUntil) {
            this.locked = locked;
            this.lockUntil = lockUntil;
        }
    }

    public SanctionResult recordLatePaymentViolation(int bidderId, String reason) {
        createTableIfMissing();
        String selectSql = "SELECT violation_count FROM bidder_penalties WHERE bidder_id = ?";
        String insertSql = "INSERT INTO bidder_penalties (bidder_id, violation_count, lock_until, last_reason) VALUES (?, ?, ?, ?)";
        String updateSql = "UPDATE bidder_penalties SET violation_count = ?, lock_until = ?, last_reason = ?, updated_at = CURRENT_TIMESTAMP WHERE bidder_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            int newCount = 1;
            try (PreparedStatement select = conn.prepareStatement(selectSql)) {
                select.setInt(1, bidderId);
                try (ResultSet rs = select.executeQuery()) {
                    if (rs.next()) {
                        newCount = rs.getInt("violation_count") + 1;
                    }
                }
            }

            boolean permanent = newCount >= 3;
            LocalDateTime lockUntil = null;
            if (!permanent) {
                int days = (newCount == 1) ? 3 : 7;
                lockUntil = LocalDateTime.now().plusDays(days);
            }

            if (newCount == 1) {
                try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                    insert.setInt(1, bidderId);
                    insert.setInt(2, newCount);
                    if (lockUntil != null) {
                        insert.setTimestamp(3, Timestamp.valueOf(lockUntil));
                    } else {
                        insert.setTimestamp(3, null);
                    }
                    insert.setString(4, reason);
                    insert.executeUpdate();
                }
            } else {
                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    update.setInt(1, newCount);
                    if (lockUntil != null) {
                        update.setTimestamp(2, Timestamp.valueOf(lockUntil));
                    } else {
                        update.setTimestamp(2, null);
                    }
                    update.setString(3, reason);
                    update.setInt(4, bidderId);
                    update.executeUpdate();
                }
            }
            if (permanent) {
                new UserDao().updateStatusById(bidderId, "LOCKED");
            } else {
                new UserDao().updateTemporaryLockById(bidderId, lockUntil);
            }
            return new SanctionResult(newCount, permanent, lockUntil);
        } catch (Exception e) {
            logger.error("Khong the ghi nhan vi pham bidder {}.", bidderId, e);
            return new SanctionResult(0, false, null);
        }
    }

    public LockInfo getTemporaryLockInfo(int bidderId) {
        createTableIfMissing();
        String sql = "SELECT lock_until FROM bidder_penalties WHERE bidder_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bidderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return new LockInfo(false, null);
                }
                Timestamp ts = rs.getTimestamp("lock_until");
                if (ts == null) {
                    return new LockInfo(false, null);
                }
                LocalDateTime until = ts.toLocalDateTime();
                return new LockInfo(until.isAfter(LocalDateTime.now()), until);
            }
        } catch (Exception e) {
            logger.error("Khong the lay trang thai tam khoa bidder {}.", bidderId, e);
            return new LockInfo(false, null);
        }
    }

    private void createTableIfMissing() {
        String sql = "CREATE TABLE IF NOT EXISTS bidder_penalties ("
                + "bidder_id INT PRIMARY KEY, "
                + "violation_count INT NOT NULL DEFAULT 0, "
                + "lock_until DATETIME NULL, "
                + "last_reason VARCHAR(255) NULL, "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.execute();
        } catch (Exception e) {
            logger.error("Khong the tao bang bidder_penalties.", e);
        }
    }
}
