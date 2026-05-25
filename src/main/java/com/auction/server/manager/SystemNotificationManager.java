package com.auction.server.manager;

import com.auction.server.dao.SystemNotificationDao;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lưu notification trước khi đẩy realtime cho user đang online.
 */
public class SystemNotificationManager {
    private static volatile SystemNotificationManager instance;
    private static final DateTimeFormatter NOTIFICATION_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final SystemNotificationDao notificationDao = new SystemNotificationDao();

    private SystemNotificationManager() {
    }

    public static SystemNotificationManager getInstance() {
        if (instance == null) {
            synchronized (SystemNotificationManager.class) {
                if (instance == null) {
                    instance = new SystemNotificationManager();
                }
            }
        }
        return instance;
    }

    public void sendPrivateNotification(int auctionId, int recipientId, String message, boolean paymentRequired) {
        long notificationId = notificationDao.saveNotification(auctionId, recipientId, message, paymentRequired);
        if (notificationId <= 0) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .error("Khong luu duoc thong bao he thong. auctionId={}, recipientId={}", auctionId, recipientId);
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("notificationId", notificationId);
        payload.addProperty("targetUserId", recipientId);
        payload.addProperty("auctionId", auctionId);
        payload.addProperty("message", message);
        payload.addProperty("sentAt", LocalDateTime.now().format(NOTIFICATION_TIME_FORMAT));
        payload.addProperty("paymentRequired", paymentRequired);
        UserManager.getInstance().sendSystemNotification(recipientId, payload);
    }
}
