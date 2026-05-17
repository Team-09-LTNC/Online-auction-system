package com.auction.client.controller.components;

import com.auction.client.networkclient.ClientSocket;
import com.auction.client.controller.auth.LoginController;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class ChatController {

    @FXML private HBox hboxSellerTarget;
    @FXML private ComboBox<String> comboChatTarget;
    @FXML private ListView<String> lvChatMessages;
    @FXML private TextField txtMessageInput;
    @FXML private Button btnSendMessage;

    // Biến static để file PushHandler từ tầng mạng có thể tìm thấy và ném tin nhắn vào
    public static ChatController instance;
    private final int currentAuctionId = 1; // Mock tạm ID phiên đấu giá hiện tại bằng 1

    @FXML
    public void initialize() {
        instance = this;

        // Cấu hình danh sách mục tiêu gửi cho Seller
        if (comboChatTarget != null) {
            comboChatTarget.setItems(FXCollections.observableArrayList("Gửi tất cả mọi người", "Chỉ gửi người đang theo dõi"));
            comboChatTarget.setValue("Gửi tất cả mọi người");
        }

        // 🔥 PHÂN QUYỀN GIAO DIỆN CHAT: Check vai trò của tài khoản
        if (LoginController.roleComboBoxStatic != null) {
            String role = LoginController.roleComboBoxStatic.getValue();
            if (role == null || !role.contains("Seller")) {
                // Nếu là Bidder (Người mua), ẩn hoàn toàn cái cụm chọn mục tiêu gửi đi để tránh nhầm lẫn
                if (hboxSellerTarget != null) {
                    hboxSellerTarget.setVisible(false);
                    hboxSellerTarget.setManaged(false);
                }
            }
        }

        // Tạo sẵn vài tin nhắn mock mẫu cho sinh động
        lvChatMessages.getItems().addAll(
                "[Hệ thống]: Chào mừng bạn tham gia phòng trao đổi trực tuyến!",
                "Mạnh Hùng: Xe này máy móc còn nguyên bản không chủ thớt ơi?",
                "Nguyễn An: Nhìn quả nội thất chất đấy, tí tôi vào gom lúa đua xe!"
        );
    }

    @FXML
    private void handleSendMessage(ActionEvent event) {
        String message = txtMessageInput.getText().trim();
        if (message.isEmpty()) return;

        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("type", "SEND_CHAT_MESSAGE");
        jsonRequest.addProperty("auctionId", currentAuctionId);
        jsonRequest.addProperty("message", message);

        // Kiểm tra xem có phải là người bán đang chọn chế độ gửi riêng không
        if (LoginController.roleComboBoxStatic != null && "Seller".contains(LoginController.roleComboBoxStatic.getValue())) {
            String target = comboChatTarget.getValue();
            if ("Chỉ gửi người đang theo dõi".equals(target)) {
                jsonRequest.addProperty("chatTarget", "FOLLOWERS_ONLY");
            } else {
                jsonRequest.addProperty("chatTarget", "ALL");
            }
        } else {
            jsonRequest.addProperty("chatTarget", "ALL"); // Người mua mặc định luôn là gửi tất cả
        }

        btnSendMessage.setDisable(true);

        // Bắn Socket qua ClientSocket lên Server
        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "SEND_CHAT_RESPONSE", response -> {
            Platform.runLater(() -> {
                btnSendMessage.setDisable(false);
                if (response.has("success") && response.get("success").getAsBoolean()) {
                    txtMessageInput.clear();
                    // Lưu ý: Không cần tự add tin nhắn vào ListView ở đây,
                    // vì Server sẽ tự động bắn ngược lại cho chính mình qua đường Push để hiển thị đồng bộ!
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Không thể gửi tin nhắn!");
                    alert.showAndWait();
                }
            });
        });
    }

    /**
     * HÀM REAL-TIME: Được gọi từ PushHandler khi có tin nhắn mới từ bất kỳ ai đổ về
     */
    public void receiveIncomingMessage(String senderName, String msgContent, boolean isBroadcastToFollowers) {
        Platform.runLater(() -> {
            String logEntry;
            if (isBroadcastToFollowers) {
                logEntry = String.format("📢 [CHỦ PHÒNG GỬI ĐẾN NGƯỜI THEO DÕI] %s: %s", senderName, msgContent);
            } else {
                logEntry = String.format("%s: %s", senderName, msgContent);
            }
            lvChatMessages.getItems().add(logEntry);

            // Tự động cuộn ListView xuống dòng cuối cùng để người dùng dễ đọc
            lvChatMessages.scrollTo(lvChatMessages.getItems().size() - 1);
        });
    }
}