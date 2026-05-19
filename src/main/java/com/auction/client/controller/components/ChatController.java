package com.auction.client.controller.components;

import com.auction.client.networkclient.ClientSocket;
import com.auction.client.controller.auth.UserSession;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @FXML private HBox hboxSellerTarget;
    @FXML private ComboBox<String> comboChatTarget;
    @FXML private ListView<String> lvChatMessages;
    @FXML private TextField txtMessageInput;
    @FXML private Button btnSendMessage;
    @FXML private ListView<String> lvChatRooms;
    @FXML private VBox chatArea;
    @FXML private Label lblRoomName;

    public static ChatController instance;
    private int currentAuctionId = -1; 
    
    // Lưu tạm danh sách ID phòng chat tương ứng với item trong ListView
    private final java.util.Map<String, Integer> roomMap = new java.util.HashMap<>();

    @FXML
    public void initialize() {
        instance = this;
        chatArea.setVisible(false); // Ẩn vùng chat đi cho đến khi chọn phòng
        
        if (comboChatTarget != null) {
            comboChatTarget.setItems(FXCollections.observableArrayList("Gửi tất cả mọi người", "Chỉ gửi người đang theo dõi"));
            comboChatTarget.setValue("Gửi tất cả mọi người");
        }

        String role = UserSession.getCurrentRole();
        if (role != null) {
            if (!"SELLER".equalsIgnoreCase(role)) {
                if (hboxSellerTarget != null) {
                    hboxSellerTarget.setVisible(false);
                    hboxSellerTarget.setManaged(false);
                }
            }
        }

        // Sự kiện khi click vào một phòng trong ListView
        if (lvChatRooms != null) {
            lvChatRooms.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && roomMap.containsKey(newValue)) {
                    openChatRoom(roomMap.get(newValue), newValue);
                }
            });
        }

        fetchJoinedRooms();
    }

    private void fetchJoinedRooms() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_JOINED_AUCTIONS);

        ClientSocket.getInstance().sendJsonRequest(request, "JOINED_AUCTIONS_RESPONSE", response -> {
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean() && response.has("auctions")) {
                    JsonArray auctions = response.getAsJsonArray("auctions");
                    lvChatRooms.getItems().clear();
                    roomMap.clear();
                    
                    for (JsonElement element : auctions) {
                        JsonObject obj = element.getAsJsonObject();
                        int auctionId = obj.has("auctionId") ? obj.get("auctionId").getAsInt() : -1;
                        String name = obj.has("itemName") ? obj.get("itemName").getAsString() : "Phiên Đấu Giá";
                        
                        if (auctionId != -1) {
                            String displayTxt = "💬 " + name;
                            roomMap.put(displayTxt, auctionId);
                            lvChatRooms.getItems().add(displayTxt);
                        }
                    }
                    if (lvChatRooms.getItems().isEmpty()) {
                        lvChatRooms.getItems().add("Chưa tham gia phiên nào");
                        lvChatRooms.setDisable(true);
                    } else {
                        lvChatRooms.setDisable(false);
                    }
                }
            });
        });
    }

    private void openChatRoom(int auctionId, String roomName) {
        this.currentAuctionId = auctionId;
        chatArea.setVisible(true);
        lblRoomName.setText(roomName.replace("💬 ", ""));
        
        lvChatMessages.getItems().clear();
        lvChatMessages.getItems().add("[Hệ thống]: Chào mừng bạn tham gia phòng trao đổi trực tuyến của " + lblRoomName.getText());

        // Gửi lệnh tham gia phòng để Server add vào Observer (nếu chưa)
        JsonObject joinReq = new JsonObject();
        joinReq.addProperty("type", ActionType.JOIN_AUCTION);
        joinReq.addProperty("auctionId", auctionId);
        ClientSocket.getInstance().sendJsonRequest(joinReq, null, null);
    }

    public void setAuctionId(int auctionId) {
        this.currentAuctionId = auctionId;
    }

    @FXML
    private void handleSendMessage(ActionEvent event) {
        if (currentAuctionId == -1) return;
        
        String message = txtMessageInput.getText().trim();
        if (message.isEmpty()) return;

        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("type", ActionType.SEND_CHAT_MESSAGE);
        jsonRequest.addProperty("auctionId", currentAuctionId);
        jsonRequest.addProperty("message", message);

        if (btnSendMessage != null) btnSendMessage.setDisable(true);

        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "CHAT_SEND_RESPONSE", response -> {
            Platform.runLater(() -> {
                if (btnSendMessage != null) btnSendMessage.setDisable(false);

                boolean success = response.has("success") && response.get("success").getAsBoolean();
                if (success) {
                    if (txtMessageInput != null) txtMessageInput.clear();
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Không thể gửi tin nhắn!");
                    alert.showAndWait();
                }
            });
        });
    }

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
            lvChatMessages.scrollTo(lvChatMessages.getItems().size() - 1);
        });
    }

    public static void shutdown() {
        instance = null;
    }
}