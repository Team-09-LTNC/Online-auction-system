package com.auction.server.dao;

import com.auction.server.db.DatabaseConnection;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notification riêng được lưu trong chat_messages với admin hệ thống làm sender.
 */
public class SystemNotificationDao {
    private static final Logger logger = LoggerFactory.getLogger(SystemNotificationDao.class);
    private static final int SYSTEM_ADMIN_ID = 1;

    public long luuThongBao(int auctionId, int recipientId, String message, boolean paymentRequired) {
        String sql = "INSERT INTO chat_messages "
                + "(auction_id, sender_id, recipient_id, message, payment_required) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, auctionId);
            pstmt.setInt(2, SYSTEM_ADMIN_ID);
            pstmt.setInt(3, recipientId);
            pstmt.setString(4, message);
            pstmt.setBoolean(5, paymentRequired);
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        } catch (Exception e) {
            logger.error("Không lưu được notification cho user {} ở phiên {}.", recipientId, auctionId, e);
            return -1;
        }
    }

    public JsonArray layThongBaoCuaNguoiNhan(int recipientId) {
        String sql = "SELECT cm.id, cm.auction_id, cm.message, cm.payment_required, cm.send_time, a.status "
                + "FROM chat_messages cm "
                + "JOIN auctions a ON a.id = cm.auction_id "
                + "WHERE cm.sender_id = ? AND cm.recipient_id = ? "
                + "ORDER BY cm.send_time DESC, cm.id DESC LIMIT 100";
        JsonArray notifications = new JsonArray();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, SYSTEM_ADMIN_ID);
            pstmt.setInt(2, recipientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject notification = new JsonObject();
                    notification.addProperty("notificationId", rs.getLong("id"));
                    notification.addProperty("auctionId", rs.getInt("auction_id"));
                    notification.addProperty("message", rs.getString("message"));
                    notification.addProperty("sentAt", rs.getTimestamp("send_time").toString());
                    notification.addProperty("paymentRequired",
                            rs.getBoolean("payment_required") && "FINISHED".equalsIgnoreCase(rs.getString("status")));
                    notifications.add(notification);
                }
            }
        } catch (Exception e) {
            logger.error("Không tải được notification cho user {}.", recipientId, e);
        }
        return notifications;
    }
}
