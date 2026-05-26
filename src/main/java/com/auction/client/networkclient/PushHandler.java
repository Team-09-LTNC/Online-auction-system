package com.auction.client.networkclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.common.enums.ActionType;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Xử lý các gói tin server tự gửi về (không kèm requestId)
 * Tách riêng khỏi Socket để dễ mở rộng
 */
public class PushHandler {

    private static final Logger logger = LoggerFactory.getLogger(PushHandler.class);
    private static final List<JsonObject> pendingNotifications = new CopyOnWriteArrayList<>();
    private static boolean auctionRefreshScheduled;

    public static com.auction.client.controller.bidder.AuctionRoomController currentRoomController;

    public static void handle(String type, JsonObject payload) {
        switch (type) {
            case ActionType.AUCTION_BID_UPDATE -> onBidUpdate(payload);
            case ActionType.AUCTION_RESULT -> onAuctionResult(payload);
            case ActionType.AUCTION_CHANGED -> onAuctionChanged(payload);
            case "SYSTEM_NOTIFICATION" -> onSystemNotification(payload);
            default -> logger.error("[PushHandler] Unknown push type: {}", type);
        }
    }

    private static void onBidUpdate(JsonObject payload) {
        try {
            JsonObject transaction = payload.getAsJsonObject("transaction");
            long newBidAmount = transaction.get("bidAmount").getAsLong();
            int auctionId = payload.has("auctionId") && !payload.get("auctionId").isJsonNull()
                    ? payload.get("auctionId").getAsInt()
                    : transaction.has("auctionId") && !transaction.get("auctionId").isJsonNull()
                    ? transaction.get("auctionId").getAsInt()
                    : -1;
            String bidTime = transaction.has("timestamp") && !transaction.get("timestamp").isJsonNull()
                    ? transaction.get("timestamp").getAsString()
                    : null;

            JsonObject bidder = transaction.getAsJsonObject("bidder");
            String bidderName = "Unknown";
            if (bidder != null) {
                if (bidder.has("fullName") && !bidder.get("fullName").isJsonNull()
                        && !bidder.get("fullName").getAsString().isBlank()) {
                    bidderName = bidder.get("fullName").getAsString();
                } else if (bidder.has("username") && !bidder.get("username").isJsonNull()
                        && !bidder.get("username").getAsString().isBlank()) {
                    bidderName = bidder.get("username").getAsString();
                }
            }
            final String finalBidderName = bidderName;
            String endTime = payload.has("endTime") && !payload.get("endTime").isJsonNull()
                    ? payload.get("endTime").getAsString()
                    : null;
            String serverNow = payload.has("serverNow") && !payload.get("serverNow").isJsonNull()
                    ? payload.get("serverNow").getAsString()
                    : null;
            String status = payload.has("status") && !payload.get("status").isJsonNull()
                    ? payload.get("status").getAsString()
                    : null;

            Platform.runLater(() -> {
                logger.info("[Push] Giá mới: {} đ bởi {}", newBidAmount, finalBidderName);
                if (currentRoomController != null) {
                    currentRoomController.updateRealtimeBid(
                            newBidAmount,
                            finalBidderName,
                            endTime,
                            serverNow,
                            status,
                            auctionId,
                            bidTime
                    );
                }
                requestAuctionViewsRefresh();
            });
        } catch (Exception e) {
            logger.error("[PushHandler] Lỗi bóc tách dữ liệu AUCTION_BID_UPDATE: {}", e.getMessage());
        }
    }

    private static void onAuctionResult(JsonObject payload) {
        try {
            String newStatus = payload.get("newStatus").getAsString();
            Platform.runLater(() -> {
                logger.info("[Push] Phiên kết thúc! Trạng thái mới: {}", newStatus);
                if (currentRoomController != null) {
                    currentRoomController.updateRealtimeStatus(newStatus);
                }
                requestAuctionViewsRefresh();
            });
        } catch (Exception e) {
            logger.error("[PushHandler] Lỗi bóc tách dữ liệu AUCTION_RESULT: {}", e.getMessage());
        }
    }

    private static void onAuctionChanged(JsonObject payload) {
        Platform.runLater(() -> {
            String reason = payload.has("reason") && !payload.get("reason").isJsonNull()
                    ? payload.get("reason").getAsString()
                    : "UNKNOWN";
            logger.info("[Push] Dữ liệu phiên đấu giá thay đổi: {}", reason);
            requestAuctionViewsRefresh();
        });
    }

    public static void requestAuctionViewsRefresh() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(PushHandler::requestAuctionViewsRefresh);
            return;
        }
        if (auctionRefreshScheduled) {
            return;
        }
        auctionRefreshScheduled = true;
        PauseTransition delay = new PauseTransition(Duration.millis(250));
        delay.setOnFinished(event -> {
            auctionRefreshScheduled = false;
            com.auction.client.util.AuctionWarmupCache.clear();
            if (com.auction.client.controller.MainController.instance != null) {
                com.auction.client.controller.MainController.instance.refreshRealtimeContent();
            }
            if (com.auction.client.controller.admin.AdminLayoutController.instance != null) {
                com.auction.client.controller.admin.AdminLayoutController.instance.refreshRealtimeContent();
            }
        });
        delay.play();
    }
    private static void onSystemNotification(JsonObject payload) {
        String targetRole = payload.has("targetRole") ? payload.get("targetRole").getAsString() : "ALL";
        String myRole = com.auction.client.controller.auth.UserSession.getCurrentRole();
        int myUserId = com.auction.client.controller.auth.UserSession.getUserId();
        if (payload.has("targetUserId")
                && !payload.get("targetUserId").isJsonNull()
                && payload.get("targetUserId").getAsInt() != myUserId) {
            return;
        }

        if ("ALL".equalsIgnoreCase(targetRole) || (myRole != null && myRole.equalsIgnoreCase(targetRole))) {
            Platform.runLater(() -> {
                boolean notificationTabVisible = com.auction.client.controller.MainController.instance != null
                        && com.auction.client.controller.MainController.instance.getCurrentCenterController()
                        instanceof com.auction.client.controller.components.ChatController;
                if (!notificationTabVisible) {
                    com.auction.client.controller.components.SidebarController.recordUnreadNotification();
                }
                if (com.auction.client.controller.components.ChatController.instance != null) {
                    com.auction.client.controller.components.ChatController.instance.receiveNotification(payload);
                } else {
                    pendingNotifications.add(payload.deepCopy());
                }
            });
        }
    }

    public static void flushNotifications(com.auction.client.controller.components.ChatController chatController) {
        for (JsonObject payload : pendingNotifications) {
            chatController.receiveNotification(payload);
            pendingNotifications.remove(payload);
        }
    }

    public static void clearNotifications() {
        pendingNotifications.clear();
    }
}
