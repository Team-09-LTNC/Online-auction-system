
package com.auction.client.network;

import com.auction.common.dto.AuctionDTOs;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý các gói tin server tự gửi về (không kèm requestId)
 */
public class PushHandler {

    private static final Logger logger = LoggerFactory.getLogger(PushHandler.class);
    private static final Gson gson = new Gson();

    public static com.auction.client.controller.bidder.AuctionRoomController currentRoomController;

    public static void handle(String type, String rawJson) {
        if ("NEW_BID_UPDATE".equals(type) || "AUCTION_BID_UPDATE".equals(type)) {
            AuctionDTOs.AuctionUpdateDTO push = gson.fromJson(rawJson, AuctionDTOs.AuctionUpdateDTO.class);
            onBidUpdate(push);
        } else if ("AUCTION_RESULT".equals(type)) {
            JsonObject pushData = gson.fromJson(rawJson, JsonObject.class);
            onAuctionResult(pushData);
        } else if ("CHAT_MESSAGE_PUSH".equals(type)) {
            JsonObject chatPush = gson.fromJson(rawJson, JsonObject.class);
            onChatMessageReceived(chatPush);
        } else {
            logger.error("[PushHandler] Unknown push type: {}", type);
        }
    }

    private static void onBidUpdate(AuctionDTOs.AuctionUpdateDTO push) {
        if (push != null) {
            long newPrice = push.getCurrentPrice();
            String bidderName = push.getHighestBidderName();

            logger.info("[Push Server] Giá mới: {} đ bởi {}", newPrice, bidderName);

            if (currentRoomController != null) {
                currentRoomController.updateRealtimeBid(newPrice, bidderName);
            }
        }
    }

    private static void onAuctionResult(JsonObject pushData) {
        if (pushData != null) {
            String winner = pushData.has("winnerName") ? pushData.get("winnerName").getAsString() : "Không có";
            long finalPrice = pushData.has("finalPrice") ? pushData.get("finalPrice").getAsLong() : 0;

            logger.info("[Push Server] Phiên kết thúc! Người thắng: {} – Giá: {}", winner, finalPrice);

            if (currentRoomController != null) {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("KẾT THÚC PHIÊN");
                    alert.setHeaderText("Phiên đấu giá đã khép lại!");
                    alert.setContentText(String.format("Chúc mừng người chiến thắng: %s\nMức giá chốt hạ: %,d đ", winner, finalPrice));
                    alert.showAndWait();
                });
            }
        }
    }

    /**
     * Hàm xử lý tin nhắn chat đổ về từ Server qua đường Push ngầm
     */
    private static void onChatMessageReceived(JsonObject chatPush) {
        if (chatPush != null && chatPush.has("senderName") && chatPush.has("message")) {
            String sender = chatPush.get("senderName").getAsString();
            String content = chatPush.get("message").getAsString();

            boolean isSystem = chatPush.has("isSystem") && chatPush.get("isSystem").getAsBoolean();

            String chatTarget = chatPush.has("chatTarget") ? chatPush.get("chatTarget").getAsString() : "ALL";
            if ("FOLLOWERS_ONLY".equals(chatTarget)) {
                content = " [CHỦ PHÒNG GỬI ĐẾN NGƯỜI THEO DÕI] " + content;
            }

            logger.info("[Push Chat] Nhận tin nhắn từ {}: {}", sender, content);

            if (com.auction.client.controller.components.ChatController.instance != null) {
                com.auction.client.controller.components.ChatController.instance.receiveIncomingMessage(sender, content, isSystem);
            }
        }
    }
}