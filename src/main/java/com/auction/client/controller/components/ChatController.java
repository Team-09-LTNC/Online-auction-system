package com.auction.client.controller.components;

import com.auction.client.networkclient.ClientSocket;
import com.auction.client.controller.auth.UserSession; // Import Session mới chuẩn chỉ
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @FXML private HBox hboxSellerTarget;
    @FXML private ComboBox<String> comboChatTarget;
    @FXML private ListView<String> lvChatMessages;
    @FXML private TextField txtMessageInput;
    @FXML private Button btnSendMessage;

    // Biến static để file PushHandler/ClientSocket từ tầng mạng có thể tìm thấy và ném tin nhắn vào
    public static ChatController instance;
    private final int currentAuctionId = 1; // Mock tạm ID phiên đấu giá hiện tại bằng 1

    @FXML
    public void initialize() {
        instance = this;
        logger.info("[Chat] Đã khởi tạo phòng Chat cho phiên đấu giá ID: {}", currentAuctionId);

        // Cấu hình danh sách mục tiêu gửi cho Seller
        if (comboChatTarget != null) {
            comboChatTarget.setItems(FXCollections.observableArrayList("Gửi tất cả mọi người", "Chỉ gửi người đang theo dõi"));
            comboChatTarget.setValue("Gửi tất cả mọi người");
        }

        // Lấy vai trò (Role) an toàn từ UserSession để phân quyền giao diện Chat
        String role = UserSession.getCurrentRole();

        if (role != null) {
            // So sánh chuẩn chỉ với ENUM viết hoa "SELLER" để phân quyền
            if (!"SELLER".equalsIgnoreCase(role)) {
                // Nếu là Bidder (Người mua), ẩn hoàn toàn cái cụm chọn mục tiêu gửi đi để tránh nhầm lẫn
                if (hboxSellerTarget != null) {
                    hboxSellerTarget.setVisible(false);
                    hboxSellerTarget.setManaged(false);
                    logger.info("[Chat] Đã ẩn thanh chọn mục tiêu gửi tin nhắn đối với vai trò Người mua (BIDDER).");
                }
            }
        }

        // Dọn sạch tin nhắn cũ khi vào phòng để chuẩn bị hứng luồng dữ liệu real-time
        if (lvChatMessages != null) {
            lvChatMessages.getItems().clear();
            lvChatMessages.getItems().add("[Hệ thống]: Chào mừng bạn tham gia phòng trao đổi trực tuyến!");
        }
    }

    @FXML
    private void handleSendMessage(ActionEvent event) {
        String message = txtMessageInput.getText().trim();
        if (message.isEmpty()) return;

        // ĐÓNG GÓI RUỘT DỮ LIỆU ĐỘNG 100% GỬI LÊN SERVER
        JsonObject jsonRequest = new JsonObject();

        // Đổi từ "type" thành "action" để đồng bộ với cấu hình Socket toàn hệ thống
        jsonRequest.addProperty("action", "SEND_CHAT_MESSAGE");
        jsonRequest.addProperty("roomId", currentAuctionId);
        jsonRequest.addProperty("message", message);

        jsonRequest.addProperty("userId", UserSession.getUserId());
        jsonRequest.addProperty("username", UserSession.getUsername());

        String role = UserSession.getCurrentRole();
        if (role != null) {
            if ("SELLER".equalsIgnoreCase(role) && comboChatTarget != null) {
                String target = comboChatTarget.getValue();
                if ("Chỉ gửi người đang theo dõi".equals(target)) {
                    jsonRequest.addProperty("chatTarget", "FOLLOWERS_ONLY");
                } else {
                    jsonRequest.addProperty("chatTarget", "ALL");
                }
            } else {
                jsonRequest.addProperty("chatTarget", "ALL"); // Người mua mặc định luôn là gửi tất cả
            }
        } else {
            jsonRequest.addProperty("chatTarget", "ALL");
        }

        if (btnSendMessage != null) btnSendMessage.setDisable(true);

        // Bắn Socket qua ClientSocket lên Server của Kiên
        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "SEND_CHAT_RESPONSE", response -> {
            Platform.runLater(() -> {
                if (btnSendMessage != null) btnSendMessage.setDisable(false);

                boolean success = response.has("success") && response.get("success").getAsBoolean();
                if (success) {
                    if (txtMessageInput != null) txtMessageInput.clear(); // Xóa chữ ô nhập khi gửi thành công
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Không thể gửi tin nhắn!");
                    alert.showAndWait();
                }
            });
        });
    }

    /**
     * HÀM REAL-TIME: Được gọi từ PushHandler tầng mạng khi có gói PUSH_CHAT_MESSAGE đổ về
     */
    public void receiveIncomingMessage(String senderName, String msgContent, boolean isSystem) {
        if (lvChatMessages == null) return;

        Platform.runLater(() -> {
            String logEntry;
            if (isSystem) {
                logEntry = String.format("[Hệ thống]: %s", msgContent);
            } else {
                logEntry = String.format("%s: %s", senderName, msgContent);
            }
            lvChatMessages.getItems().add(logEntry);

            // Tự động cuộn ListView xuống dòng cuối cùng để người dùng dễ đọc, không cần kéo tay
            lvChatMessages.scrollTo(lvChatMessages.getItems().size() - 1);
        });
    }

    /**
     * HÀM GIẢI PHÓNG BỘ NHỚ: Gọi hàm này khi người dùng bấm nút đóng/thoát phòng đấu giá
     */
    public static void shutdown() {
        instance = null;
        logger.info("[Chat] Đã giải phóng instance ChatController để sẵn sàng cho phòng đấu giá mới.");
    }
}