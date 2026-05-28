package com.auction.server.dao;

import com.auction.server.db.DatabaseConnection;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thông báo hệ thống được lưu trong chat_messages với người gửi ADMIN.
 */
public class SystemNotificationDao {
    private static final Logger logger = LoggerFactory.getLogger(SystemNotificationDao.class);
    private static final DateTimeFormatter NOTIFICATION_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final AtomicBoolean checkedLocalSentAtColumn = new AtomicBoolean(false);

    public long saveNotification(
            int auctionId,
            int recipientId,
            String message,
            boolean paymentRequired,
            LocalDateTime sentAt
    ) {
        Integer systemAdminId = findSystemAdminId();
        if (systemAdminId == null) {
            logger.error("Cannot find ADMIN user to send system notification.");
            return -1;
        }
        if (hasRecentDuplicateNotification(auctionId, recipientId, message, paymentRequired)) {
            logger.info("Skip duplicated notification for user {} auction {}.", recipientId, auctionId);
            return -1;
        }

        ensureLocalSentAtColumn();
        String sentAtText = sentAt.format(NOTIFICATION_TIME_FORMAT);
        String sql = "INSERT INTO chat_messages "
                + "(auction_id, sender_id, recipient_id, message, payment_required, send_time, sent_at_local) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
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
            pstmt.setTimestamp(6, Timestamp.valueOf(sentAt));
            pstmt.setString(7, sentAtText);
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        } catch (Exception e) {
            logger.error("Cannot save notification for user {} in auction {}.", recipientId, auctionId, e);
            return -1;
        }
    }

    public JsonArray getNotificationsForRecipient(int recipientId) {
        ensureLocalSentAtColumn();
        String sql = "SELECT cm.id, cm.auction_id, cm.message, cm.payment_required, "
                + "cm.send_time, cm.sent_at_local, cm.is_read, a.status "
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
                    notification.addProperty("sentAt", readSentAt(rs));
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

    public int countUnreadNotifications(int recipientId) {
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

    public boolean markAsRead(int recipientId) {
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

    private Integer findSystemAdminId() {
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

    private boolean hasRecentDuplicateNotification(int auctionId, int recipientId, String message, boolean paymentRequired) {
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

    private void ensureLocalSentAtColumn() {
        if (checkedLocalSentAtColumn.get()) {
            return;
        }

        synchronized (SystemNotificationDao.class) {
            if (checkedLocalSentAtColumn.get()) {
                return;
            }

            try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                try (ResultSet columns = metaData.getColumns(null, null, "chat_messages", "sent_at_local")) {
                    if (columns.next()) {
                        checkedLocalSentAtColumn.set(true);
                        return;
                    }
                }

                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE chat_messages ADD COLUMN sent_at_local VARCHAR(19) NULL");
                }
                checkedLocalSentAtColumn.set(true);
            } catch (Exception e) {
                logger.error("Cannot ensure local sentAt column for system notifications.", e);
            }
        }
    }

    private String readSentAt(ResultSet rs) {
        try {
            String sentAtLocal = rs.getString("sent_at_local");
            if (sentAtLocal != null && !sentAtLocal.isBlank()) {
                return sentAtLocal;
            }
        } catch (Exception ignored) {
        }

        return readSendTime(rs);
    }

    private String readSendTime(ResultSet rs) {
        try {
            LocalDateTime sentAt = rs.getObject("send_time", LocalDateTime.class);
            if (sentAt != null) {
                return sentAt.format(NOTIFICATION_TIME_FORMAT);
            }
        } catch (Exception ignored) {
        }

        try {
            Timestamp timestamp = rs.getTimestamp("send_time");
            if (timestamp != null) {
                return timestamp.toLocalDateTime().format(NOTIFICATION_TIME_FORMAT);
            }
        } catch (Exception ignored) {
        }

        return LocalDateTime.now().format(NOTIFICATION_TIME_FORMAT);
    }
}
