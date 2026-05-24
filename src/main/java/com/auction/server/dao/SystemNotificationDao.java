package com.auction.server.dao;

import com.auction.server.db.DatabaseConnection;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;

/**
 * System notifications are stored in chat_messages with an ADMIN sender.
 */
public class SystemNotificationDao {
    private static final Logger logger = LoggerFactory.getLogger(SystemNotificationDao.class);

    public long luuThongBao(int auctionId, int recipientId, String message, boolean paymentRequired) {
        Integer systemAdminId = timSystemAdminId();
        if (systemAdminId == null) {
            logger.error("Cannot find ADMIN user to send system notification.");
            return -1;
        }
        if (daCoThongBaoTrungGanDay(auctionId, recipientId, message, paymentRequired)) {
            logger.info("Skip duplicated notification for user {} auction {}.", recipientId, auctionId);
            return -1;
        }

        String sql = "INSERT INTO chat_messages "
                + "(auction_id, sender_id, recipient_id, message, payment_required) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (auctionId > 0) {
                pstmt.setInt(1, auctionId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setInt(2, systemAdminId);
            pstmt.setInt(3, recipientId);
            pstmt.setString(4, message);
            pstmt.setBoolean(5, paymentRequired);
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        } catch (Exception e) {
            logger.error("Cannot save notification for user {} in auction {}.", recipientId, auctionId, e);
            return -1;
        }
    }

    public JsonArray layThongBaoCuaNguoiNhan(int recipientId) {
        String sql = "SELECT cm.id, cm.auction_id, cm.message, cm.payment_required, cm.send_time, cm.is_read, a.status "
                + "FROM chat_messages cm "
                + "LEFT JOIN auctions a ON a.id = cm.auction_id "
                + "WHERE cm.recipient_id = ? "
                + "ORDER BY cm.send_time DESC, cm.id DESC LIMIT 100";
        JsonArray notifications = new JsonArray();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, recipientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject notification = new JsonObject();
                    notification.addProperty("notificationId", rs.getLong("id"));
                    int auctionId = rs.getInt("auction_id");
                    notification.addProperty("auctionId", rs.wasNull() ? -1 : auctionId);
                    notification.addProperty("message", rs.getString("message"));
                    notification.addProperty("sentAt", rs.getTimestamp("send_time").toString());
                    notification.addProperty("isRead", rs.getBoolean("is_read"));
                    notification.addProperty("auctionStatus", rs.getString("status"));
                    notification.addProperty("paymentRequired", rs.getBoolean("payment_required"));
                    notifications.add(notification);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot load notifications for user {}.", recipientId, e);
        }
        return notifications;
    }

    public int demThongBaoChuaDoc(int recipientId) {
        String sql = "SELECT COUNT(*) FROM chat_messages "
                + "WHERE recipient_id = ? AND is_read = FALSE";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, recipientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            logger.error("Cannot count unread notifications for user {}.", recipientId, e);
            return 0;
        }
    }

    public boolean danhDauDaDoc(int recipientId) {
        String sql = "UPDATE chat_messages SET is_read = TRUE "
                + "WHERE recipient_id = ? AND is_read = FALSE";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, recipientId);
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            logger.error("Cannot mark notifications as read for user {}.", recipientId, e);
            return false;
        }
    }

    private Integer timSystemAdminId() {
        String sql = "SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            logger.error("Cannot query ADMIN account.", e);
        }

        String fallbackSql = "SELECT id FROM users ORDER BY id LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(fallbackSql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                int fallbackId = rs.getInt("id");
                logger.warn("No ADMIN account found. Using fallback sender userId={}", fallbackId);
                return fallbackId;
            }
        } catch (Exception e) {
            logger.error("Cannot query fallback sender account.", e);
        }
        return null;
    }

    private boolean daCoThongBaoTrungGanDay(int auctionId, int recipientId, String message, boolean paymentRequired) {
        String sql = "SELECT 1 FROM chat_messages "
                + "WHERE auction_id <=> ? AND recipient_id = ? AND message = ? AND payment_required = ? "
                + "AND send_time >= DATE_SUB(NOW(), INTERVAL 10 SECOND) LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (auctionId > 0) {
                pstmt.setInt(1, auctionId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setInt(2, recipientId);
            pstmt.setString(3, message);
            pstmt.setBoolean(4, paymentRequired);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            logger.error("Cannot check duplicate notification.", e);
            return false;
        }
    }
}
