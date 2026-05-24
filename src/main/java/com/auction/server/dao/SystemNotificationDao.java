package com.auction.server.dao;

import com.auction.server.db.DatabaseConnection;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.sql.Connection;
import java.sql.Types;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        Integer systemAdminId = timSystemAdminId();
        if (systemAdminId == null) {
            return new JsonArray();
        }

        String sql = "SELECT cm.id, cm.auction_id, cm.message, cm.payment_required, cm.send_time, cm.is_read, a.status "
                + "FROM chat_messages cm "
                + "LEFT JOIN auctions a ON a.id = cm.auction_id "
                + "JOIN users s ON s.id = cm.sender_id "
                + "WHERE s.role = 'ADMIN' AND cm.recipient_id = ? "
                + "AND (cm.payment_required = FALSE OR cm.id = ("
                + "    SELECT MAX(cm2.id) FROM chat_messages cm2 "
                + "    JOIN users s2 ON s2.id = cm2.sender_id "
                + "    WHERE s2.role = 'ADMIN' "
                + "    AND cm2.recipient_id = cm.recipient_id "
                + "    AND cm2.auction_id = cm.auction_id "
                + "    AND cm2.payment_required = TRUE"
                + ")) "
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
                    notification.addProperty(
                            "paymentRequired",
                            rs.getBoolean("payment_required") && "FINISHED".equalsIgnoreCase(rs.getString("status"))
                    );
                    notifications.add(notification);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot load notifications for user {}.", recipientId, e);
        }
        return notifications;
    }

    public int demThongBaoChuaDoc(int recipientId) {
        Integer systemAdminId = timSystemAdminId();
        if (systemAdminId == null) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM chat_messages cm "
                + "JOIN users s ON s.id = cm.sender_id "
                + "WHERE s.role = 'ADMIN' AND cm.recipient_id = ? AND cm.is_read = FALSE "
                + "AND (cm.payment_required = FALSE OR cm.id = ("
                + "    SELECT MAX(cm2.id) FROM chat_messages cm2 "
                + "    JOIN users s2 ON s2.id = cm2.sender_id "
                + "    WHERE s2.role = 'ADMIN' "
                + "    AND cm2.recipient_id = cm.recipient_id "
                + "    AND cm2.auction_id = cm.auction_id "
                + "    AND cm2.payment_required = TRUE"
                + "))";
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
        Integer systemAdminId = timSystemAdminId();
        if (systemAdminId == null) {
            return false;
        }

        String sql = "UPDATE chat_messages cm "
                + "JOIN users s ON s.id = cm.sender_id "
                + "SET cm.is_read = TRUE "
                + "WHERE s.role = 'ADMIN' AND cm.recipient_id = ? AND cm.is_read = FALSE";
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

        // Fallback to any existing user to avoid dropping notification due to bad seed data.
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
}
