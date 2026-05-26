package com.auction.client.controller.components;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatController {
    public static ChatController instance;

    private final Set<Long> renderedNotificationIds = new HashSet<>();
    private final Set<Integer> renderedPaymentAuctionIds = new HashSet<>();
    private final ChatNotificationRenderer notificationRenderer = new ChatNotificationRenderer();

    @FXML
    private ListView<Node> lvChatMessages;
    @FXML
    private Label lblNotificationCount;
    @FXML
    private VBox emptyNotificationState;

    @FXML
    public void initialize() {
        instance = this;
        configureNotificationList();
        updateNotificationCount();
        loadSavedNotifications();
        markNotificationsRead();
        SidebarController.clearUnreadNotifications();
        com.auction.client.networkclient.PushHandler.flushNotifications(this);
    }

    public void receiveNotification(String content) {
        Platform.runLater(() -> {
            if (lvChatMessages != null) {
                addNotification(notificationRenderer.createRegularNotification(content, null), LocalDateTime.now());
            }
        });
    }

    public void receiveNotification(JsonObject payload) {
        Platform.runLater(() -> showNotification(payload));
    }

    private void showNotification(JsonObject payload) {
        if (lvChatMessages == null || isDisplayed(payload)) {
            return;
        }

        boolean paymentRequired = payload.has("paymentRequired")
                && payload.get("paymentRequired").getAsBoolean();
        String auctionStatus = payload.has("auctionStatus") && !payload.get("auctionStatus").isJsonNull()
                ? payload.get("auctionStatus").getAsString()
                : null;
        String message = payload.has("message") ? payload.get("message").getAsString() : "";
        String sentAt = payload.has("sentAt") && !payload.get("sentAt").isJsonNull()
                ? payload.get("sentAt").getAsString()
                : null;

        if (paymentRequired && payload.has("auctionId")) {
            int auctionId = payload.get("auctionId").getAsInt();
            if (!renderedPaymentAuctionIds.add(auctionId)) {
                return;
            }
            addNotification(notificationRenderer.createPaymentNotification(
                    auctionId, message, sentAt, auctionStatus, this::sendSettlement),
                    ChatTimeUtil.parseSentAtForOrder(sentAt));
        } else {
            addNotification(notificationRenderer.createRegularNotification(message, sentAt),
                    ChatTimeUtil.parseSentAtForOrder(sentAt));
        }
    }

    private boolean isDisplayed(JsonObject payload) {
        if (!payload.has("notificationId") || payload.get("notificationId").isJsonNull()) {
            return false;
        }
        return !renderedNotificationIds.add(payload.get("notificationId").getAsLong());
    }

    private void configureNotificationList() {
        if (lvChatMessages == null) {
            return;
        }
        lvChatMessages.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Node item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty ? null : item);
                setPadding(new Insets(7, 5, 7, 5));
                setStyle("-fx-background-color: transparent;");
            }
        });
        lvChatMessages.getItems().addListener(
                (javafx.collections.ListChangeListener<Node>) change -> updateNotificationCount()
        );
    }

    private void addNotification(Node notification, LocalDateTime sentAt) {
        if (lvChatMessages == null || notification == null) {
            return;
        }

        LocalDateTime orderTime = sentAt == null || LocalDateTime.MIN.equals(sentAt)
                ? LocalDateTime.now()
                : sentAt;
        notification.getProperties().put("sentAt", orderTime);

        int insertIndex = 0;
        while (insertIndex < lvChatMessages.getItems().size()) {
            LocalDateTime existingTime = getNotificationOrderTime(lvChatMessages.getItems().get(insertIndex));
            if (existingTime.isBefore(orderTime)) {
                break;
            }
            insertIndex++;
        }

        lvChatMessages.getItems().add(insertIndex, notification);
        updateNotificationCount();
    }

    private LocalDateTime getNotificationOrderTime(Node notification) {
        if (notification != null
                && notification.getProperties().get("sentAt") instanceof LocalDateTime sentAt) {
            return sentAt;
        }
        return LocalDateTime.MIN;
    }

    private void updateNotificationCount() {
        int tong = lvChatMessages == null ? 0 : lvChatMessages.getItems().size();
        if (lblNotificationCount != null) {
            lblNotificationCount.setText(tong + " thông báo");
        }
        if (emptyNotificationState != null) {
            emptyNotificationState.setManaged(tong == 0);
            emptyNotificationState.setVisible(tong == 0);
        }
    }

    private void loadSavedNotifications() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_SYSTEM_NOTIFICATIONS);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(request, "SYSTEM_NOTIFICATIONS_RESPONSE", response -> {
            Platform.runLater(() -> {
                if (!response.has("success") || !response.get("success").getAsBoolean()
                        || !response.has("data") || !response.get("data").isJsonArray()) {
                    return;
                }

                com.google.gson.JsonArray notifications = response.getAsJsonArray("data");
                List<JsonObject> sorted = new ArrayList<>();
                notifications.forEach(element -> sorted.add(element.getAsJsonObject()));
                sorted.sort(Comparator.comparing(ChatTimeUtil::parseSentAt).reversed());

                for (JsonObject notification : sorted) {
                    showNotification(notification);
                }
            });
        });
    }

    private void markNotificationsRead() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.MARK_SYSTEM_NOTIFICATIONS_READ);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());
        ClientSocket.getInstance().sendJsonRequest(request, "MARK_NOTIFICATIONS_READ_RESPONSE", null);
    }

    private void sendSettlement(
            int auctionId,
            String decision,
            ChatNotificationRenderer.PaymentControls controls
    ) {
        notificationRenderer.disableActions(controls);

        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.SETTLE_BUY_NOW);
        request.addProperty("auctionId", auctionId);
        request.addProperty("decision", decision);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(request, "BUY_NOW_SETTLEMENT_RESPONSE", response -> {
            Platform.runLater(() -> {
                boolean success = response.has("success") && response.get("success").getAsBoolean();
                String message = response.has("message") ? response.get("message").getAsString() : "Không xử lý được.";
                notificationRenderer.showSettlementResult(controls, message, success);
                if (!success) {
                    notificationRenderer.enableActions(controls);
                } else {
                    notificationRenderer.hideSettledPaymentControls(controls);
                }
            });
        });
    }

    public void shutdown() {
        notificationRenderer.shutdown();
        instance = null;
    }
}
