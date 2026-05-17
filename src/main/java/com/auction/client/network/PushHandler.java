// client/network/PushHandler.java
package com.auction.client.network;

import com.auction.common.dto.AuctionDTOs; // Sử dụng DTO chuẩn có sẵn của Kiên
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý các gói tin server tự gửi về (không kèm requestId)
 * Khớp chuẩn 100% với logic phát gói tin bất đồng bộ từ AuctionManager bên Server
 */
public class PushHandler {

    private static final Logger logger = LoggerFactory.getLogger(PushHandler.class);
    private static final Gson gson = new Gson();

    // 🔥 LIÊN KẾT BIẾN TOÀN CỤC: Giúp tầng mạng tìm thấy giao diện phòng đấu giá đang mở để ép cập nhật UI
    public static com.auction.client.controller.bidder.AuctionRoomController currentRoomController;

    public static void handle(String type, String rawJson) {
        // 🔥 ĐỔI THEO SERVER: Kiên đặt loại lệnh là "NEW_BID_UPDATE"
        if ("NEW_BID_UPDATE".equals(type) || "AUCTION_BID_UPDATE".equals(type)) {
            // Sử dụng đúng class AuctionUpdateDTO mà Kiên đã viết sẵn trong Common
            AuctionDTOs.AuctionUpdateDTO push = gson.fromJson(rawJson, AuctionDTOs.AuctionUpdateDTO.class);
            onBidUpdate(push);
        } else if ("AUCTION_RESULT".equals(type)) {
            // Gói kết thúc phiên xử lý linh hoạt bằng JsonObject để tránh sửa file Common
            JsonObject pushData = gson.fromJson(rawJson, JsonObject.class);
            onAuctionResult(pushData);
        } else if ("CHAT_MESSAGE_PUSH".equals(type)) {
            // Nhánh hứng tin nhắn chat real-time
            JsonObject chatPush = gson.fromJson(rawJson, JsonObject.class);
            onChatMessageReceived(chatPush);
        } else {
            logger.error("[PushHandler] Unknown push type: {}", type);
        }
    }

    private static void onBidUpdate(AuctionDTOs.AuctionUpdateDTO push) {
        // Bốc các trường dữ liệu theo đúng hàm Getter có sẵn của Kiên bên Common
        if (push != null) {
            long newPrice = push.getCurrentPrice();
            String bidderName = push.getHighestBidderName();

            logger.info("[Push Server] Giá mới: {} đ bởi {}", newPrice, bidderName);

            // NỐI MẠCH REAL-TIME: Ép giao diện phòng đấu giá nhảy số ngay lập tức
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

    private static void onChatMessageReceived(JsonObject chatPush) {
        if (chatPush != null && chatPush.has("senderName") && chatPush.has("message")) {
            String sender = chatPush.get("senderName").getAsString();
            String content = chatPush.get("message").getAsString();
            boolean isFollowersOnly = chatPush.has("chatTarget") && "FOLLOWERS_ONLY".equals(chatPush.get("chatTarget").getAsString());

            logger.info("[Push Chat] Nhận tin nhắn từ {}: {}", sender, content);

            if (com.auction.client.controller.components.ChatController.instance != null) {
                com.auction.client.controller.components.ChatController.instance.receiveIncomingMessage(sender, content, isFollowersOnly);
            }
        }
    }
}