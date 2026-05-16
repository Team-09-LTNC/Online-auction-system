// client/network/PushHandler.java
package com.auction.client.networkclient;

import com.auction.common.enums.ActionType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý các gói tin server tự gửi về (không kèm requestId)
 * Tách riêng khỏi NetworkManager để dễ mở rộng
 */
public class PushHandler {

    private static final Logger logger = LoggerFactory.getLogger(PushHandler.class);
    private static final Gson gson = new Gson();

    // TỐI ƯU KIẾN TRÚC: Lưu tham chiếu đến phòng đấu giá đang mở
    public static com.auction.client.controller.bidder.AuctionRoomController currentRoomController;

    public static void handle(String type, JsonObject payload) {
        switch (type) {
            case ActionType.AUCTION_BID_UPDATE -> onBidUpdate(payload);
            case ActionType.AUCTION_RESULT -> onAuctionResult(payload);
            default -> logger.error("[PushHandler] Unknown push type: {}", type);
        }
    }

    private static void onBidUpdate(JsonObject payload) {
        try {
            // Đối chiếu với Server: Server gửi đối tượng "transaction"
            JsonObject transaction = payload.getAsJsonObject("transaction");
            long newBidAmount = transaction.get("bidAmount").getAsLong();

            JsonObject bidder = transaction.getAsJsonObject("bidder");
            String bidderName = (bidder != null && bidder.has("username"))
                    ? bidder.get("username").getAsString() : "Unknown";

            // Luôn đảm bảo tác động lên UI nằm trong JavaFX Application Thread
            Platform.runLater(() -> {
                logger.info("[Push] Giá mới: {} bởi {}", newBidAmount, bidderName);
                if (currentRoomController != null) {
                    currentRoomController.updateRealtimeBid(newBidAmount, bidderName);
                }
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
                // Bạn có thể thiết lập hàm kết thúc phiên tại AuctionRoomController nếu cần
            });
        } catch (Exception e) {
            logger.error("[PushHandler] Lỗi bóc tách dữ liệu AUCTION_RESULT: {}", e.getMessage());
        }
    }
}