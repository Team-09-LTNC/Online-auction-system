package com.auction.client.controller.components;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonObject;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatController {
    public static ChatController instance;
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DB_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAYMENT_DEADLINE_HOURS = 24;
    private static final String[] PAYMENT_RULE_LINES = {
            "Quy định:",
            "- Hủy thanh toán hoặc quá hạn thanh toán: hệ thống trừ 10% giá chốt.",
            "- Vi phạm quá hạn lần 1: khóa tài khoản 3 ngày.",
            "- Vi phạm quá hạn lần 2: khóa tài khoản 7 ngày.",
            "- Từ lần 3: khóa tài khoản vĩnh viễn."
    };

    private final Set<Long> renderedNotificationIds = new HashSet<>();
    private final Set<Integer> renderedPaymentAuctionIds = new HashSet<>();
    private final List<Timeline> paymentCountdowns = new ArrayList<>();

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
                addNotification(createRegularNotification(content, null));
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
            addNotification(createPaymentNotification(auctionId, message, sentAt, auctionStatus));
        } else {
            addNotification(createRegularNotification(message, sentAt));
        }
    }

    private boolean isDisplayed(JsonObject payload) {
        if (!payload.has("notificationId") || payload.get("notificationId").isJsonNull()) {
            return false;
        }
        return !renderedNotificationIds.add(payload.get("notificationId").getAsLong());
    }

    private Node createRegularNotification(String content, String sentAt) {
        Label title = new Label("Thông báo hệ thống");
        title.setStyle("-fx-text-fill: #7A1B28; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label time = createTimeLabel(sentAt);

        Label label = new Label(content);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-text-fill: #3E2723; -fx-font-size: 13px; -fx-line-spacing: 2px;");

        VBox card = createNotificationCard();
        card.getChildren().addAll(title, time, label);
        return unwrapNotification("!", card);
    }

    private Node createPaymentNotification(int auctionId, String content, String sentAt, String auctionStatus) {
        Label title = new Label("Thông báo chiến thắng");
        title.setStyle("-fx-text-fill: #7A1B28; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label time = createTimeLabel(sentAt);

        Label body = new Label(content);
        body.setWrapText(true);
        body.setMaxWidth(Double.MAX_VALUE);
        body.setStyle("-fx-text-fill: #3E2723; -fx-font-size: 13px; -fx-line-spacing: 2px;");

        if ("PAID".equalsIgnoreCase(auctionStatus) || "CANCELED".equalsIgnoreCase(auctionStatus)) {
            Label settled = new Label(
                    "PAID".equalsIgnoreCase(auctionStatus)
                            ? "Bạn đã thanh toán thành công cho phiên này."
                            : "Bạn đã hủy thanh toán cho phiên này."
            );
            settled.setWrapText(true);
            settled.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1E8449;");

            VBox card = createNotificationCard();
            card.getChildren().addAll(title, time, body, settled);
            return unwrapNotification("✓", card);
        }

        Button cancel = new Button("Hủy thanh toán");
        cancel.setStyle("-fx-background-color: #F0ECE8; -fx-text-fill: #3E2723; "
                + "-fx-font-weight: bold; -fx-background-radius: 7; -fx-cursor: hand;");
        Button confirm = new Button("Xác nhận thanh toán");
        confirm.setStyle("-fx-background-color: #B32638; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 7; -fx-cursor: hand;");

        Label countdown = createPaymentCountdownLabel(sentAt, cancel, confirm);
        VBox rule = createPaymentRuleBox();

        Label result = new Label();
        result.setWrapText(true);
        result.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        cancel.setOnAction(event -> sendSettlement(auctionId, "CANCEL", cancel, confirm, result));
        confirm.setOnAction(event -> sendSettlement(auctionId, "CONFIRM", cancel, confirm, result));

        HBox actions = new HBox(10, cancel, confirm);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox card = createNotificationCard();
        card.getChildren().addAll(title, time, body, countdown, rule, result, actions);
        return unwrapNotification("✓", card);
    }

    private VBox createPaymentRuleBox() {
        VBox ruleBox = new VBox(4);
        ruleBox.setMaxWidth(Double.MAX_VALUE);
        ruleBox.setStyle("-fx-background-color: #FFF7E8; -fx-background-radius: 7; "
                + "-fx-border-color: #E7C27D; -fx-border-radius: 7; "
                + "-fx-padding: 8 10;");

        for (int i = 0; i < PAYMENT_RULE_LINES.length; i++) {
            Label line = new Label(PAYMENT_RULE_LINES[i]);
            line.setWrapText(true);
            line.setMaxWidth(Double.MAX_VALUE);
            line.setStyle("-fx-text-fill: #7A4B11; -fx-font-size: 12px; "
                    + "-fx-font-weight: bold; -fx-line-spacing: 2px;");
            if (i == 0) {
                line.setStyle(line.getStyle() + "-fx-font-size: 13px;");
            }
            ruleBox.getChildren().add(line);
        }

        return ruleBox;
    }

    private Label createPaymentCountdownLabel(String sentAt, Button cancel, Button confirm) {
        Label countdown = new Label();
        countdown.setWrapText(true);
        countdown.setStyle("-fx-background-color: #F7EAEC; -fx-background-radius: 7; "
                + "-fx-padding: 7 10; -fx-text-fill: #A64452; -fx-font-size: 12px; "
                + "-fx-font-weight: bold;");

        LocalDateTime sentTime = parseSentAtText(sentAt);
        if (sentTime == LocalDateTime.MIN) {
            sentTime = LocalDateTime.now();
        }
        LocalDateTime deadline = sentTime.plusHours(PAYMENT_DEADLINE_HOURS);
        updatePaymentCountdownLabel(countdown, deadline, cancel, confirm);

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event ->
                updatePaymentCountdownLabel(countdown, deadline, cancel, confirm)
        ));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        paymentCountdowns.add(timeline);

        return countdown;
    }

    private void updatePaymentCountdownLabel(Label countdown, LocalDateTime deadline, Button cancel, Button confirm) {
        java.time.Duration remaining = java.time.Duration.between(LocalDateTime.now(), deadline);
        if (!remaining.isPositive()) {
            countdown.setText("Đã hết thời hạn thanh toán. Hệ thống sẽ tự động hủy và xử lý phí phạt 10%.");
            countdown.setStyle("-fx-background-color: #F2EEEB; -fx-background-radius: 7; "
                    + "-fx-padding: 7 10; -fx-text-fill: #7D6E6A; -fx-font-size: 12px; "
                    + "-fx-font-weight: bold;");
            cancel.setDisable(true);
            confirm.setDisable(true);
            return;
        }

        long totalSeconds = remaining.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        countdown.setText(String.format(
                "Thời hạn thanh toán còn: %02d:%02d:%02d (hạn cuối %s)",
                hours,
                minutes,
                seconds,
                deadline.format(DISPLAY_TIME_FORMAT)
        ));
    }

    private Label createTimeLabel(String sentAt) {
        Label time = new Label(formatTime(sentAt));
        time.setStyle("-fx-text-fill: #8A6F66; -fx-font-size: 11px;");
        return time;
    }

    private String formatTime(String sentAt) {
        LocalDateTime parsed = parseSentAtText(sentAt);
        if (parsed == LocalDateTime.MIN) {
            return LocalDateTime.now().format(DISPLAY_TIME_FORMAT);
        }
        return parsed.format(DISPLAY_TIME_FORMAT);
    }

    private VBox createNotificationCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(12));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color: #FFFDFC; -fx-background-radius: 8; "
                + "-fx-border-color: #E8DDD5; -fx-border-radius: 8;");
        return card;
    }

    private Node unwrapNotification(String icon, VBox content) {
        Label marker = new Label(icon);
        marker.setAlignment(Pos.CENTER);
        marker.setMinSize(30, 30);
        marker.setPrefSize(30, 30);
        marker.setStyle("-fx-background-color: #F4DDE0; -fx-background-radius: 99; "
                + "-fx-text-fill: #A64452; -fx-font-size: 15px; -fx-font-weight: bold;");

        HBox row = new HBox(12, marker, content);
        row.setAlignment(Pos.TOP_LEFT);
        row.setFillHeight(true);
        HBox.setHgrow(content, javafx.scene.layout.Priority.ALWAYS);
        return row;
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

    private void addNotification(Node notification) {
        lvChatMessages.getItems().add(0, notification);
        updateNotificationCount();
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
                sorted.sort(Comparator.comparing(this::parseSentAt));

                for (JsonObject notification : sorted) {
                    showNotification(notification);
                }
            });
        });
    }

    private LocalDateTime parseSentAt(JsonObject payload) {
        if (payload == null || !payload.has("sentAt") || payload.get("sentAt").isJsonNull()) {
            return LocalDateTime.MIN;
        }

        return parseSentAtText(payload.get("sentAt").getAsString());
    }

    private LocalDateTime parseSentAtText(String sentAt) {
        if (sentAt == null || sentAt.isBlank()) {
            return LocalDateTime.MIN;
        }

        String normalized = sentAt.trim().replace('T', ' ');
        int dotIndex = normalized.indexOf('.');
        if (dotIndex > 0) {
            normalized = normalized.substring(0, dotIndex);
        }

        try {
            return LocalDateTime.parse(normalized, DB_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.MIN;
        }
    }

    private void markNotificationsRead() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.MARK_SYSTEM_NOTIFICATIONS_READ);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());
        ClientSocket.getInstance().sendJsonRequest(request, "MARK_NOTIFICATIONS_READ_RESPONSE", null);
    }

    private void sendSettlement(int auctionId, String decision, Button cancel, Button confirm, Label result) {
        cancel.setDisable(true);
        confirm.setDisable(true);

        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.SETTLE_BUY_NOW);
        request.addProperty("auctionId", auctionId);
        request.addProperty("decision", decision);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(request, "BUY_NOW_SETTLEMENT_RESPONSE", response -> {
            Platform.runLater(() -> {
                boolean success = response.has("success") && response.get("success").getAsBoolean();
                String message = response.has("message") ? response.get("message").getAsString() : "Không xử lý được.";
                result.setText(message);
                result.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: "
                        + (success ? "#1E8449;" : "#A64452;"));
                if (!success) {
                    cancel.setDisable(false);
                    confirm.setDisable(false);
                } else {
                    showResult(message);
                }
            });
        });
    }

    private void showResult(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Quyết toán phiên đấu giá");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void shutdown() {
        for (Timeline timeline : paymentCountdowns) {
            timeline.stop();
        }
        paymentCountdowns.clear();
        instance = null;
    }
}
