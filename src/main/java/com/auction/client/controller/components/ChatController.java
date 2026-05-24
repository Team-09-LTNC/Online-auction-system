package com.auction.client.controller.components;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonObject;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

public class ChatController {
    public static ChatController instance;
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DB_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Set<Long> renderedNotificationIds = new HashSet<>();
    private final Set<Integer> renderedPaymentAuctionIds = new HashSet<>();

    @FXML
    private ListView<Node> lvChatMessages;
    @FXML
    private Label lblNotificationCount;
    @FXML
    private VBox emptyNotificationState;

    @FXML
    public void initialize() {
        instance = this;
        cauHinhDanhSachThongBao();
        capNhatTongThongBao();
        taiThongBaoDaLuu();
        danhDauThongBaoDaDoc();
        SidebarController.clearUnreadNotifications();
        com.auction.client.networkclient.PushHandler.flushNotifications(this);
    }

    public void receiveNotification(String content) {
        Platform.runLater(() -> {
            if (lvChatMessages != null) {
                themThongBao(taoThongBaoThuong(content, null));
            }
        });
    }

    public void receiveNotification(JsonObject payload) {
        Platform.runLater(() -> hienThiThongBao(payload));
    }

    private void hienThiThongBao(JsonObject payload) {
        if (lvChatMessages == null || daHienThi(payload)) {
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
            themThongBao(taoThongBaoThanhToan(auctionId, message, sentAt, auctionStatus));
        } else {
            themThongBao(taoThongBaoThuong(message, sentAt));
        }
    }

    private boolean daHienThi(JsonObject payload) {
        if (!payload.has("notificationId") || payload.get("notificationId").isJsonNull()) {
            return false;
        }
        return !renderedNotificationIds.add(payload.get("notificationId").getAsLong());
    }

    private Node taoThongBaoThuong(String content, String sentAt) {
        Label title = new Label("Thông báo hệ thống");
        title.setStyle("-fx-text-fill: #7A1B28; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label time = taoNhanThoiGian(sentAt);

        Label label = new Label(content);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-text-fill: #3E2723; -fx-font-size: 13px; -fx-line-spacing: 2px;");

        VBox card = taoKhungThongBao();
        card.getChildren().addAll(title, time, label);
        return bocThongBao("!", card);
    }

    private Node taoThongBaoThanhToan(int auctionId, String content, String sentAt, String auctionStatus) {
        Label title = new Label("Thông báo chiến thắng");
        title.setStyle("-fx-text-fill: #7A1B28; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label time = taoNhanThoiGian(sentAt);

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

            VBox card = taoKhungThongBao();
            card.getChildren().addAll(title, time, body, settled);
            return bocThongBao("✓", card);
        }

        Button cancel = new Button("Hủy thanh toán");
        cancel.setStyle("-fx-background-color: #F0ECE8; -fx-text-fill: #3E2723; "
                + "-fx-font-weight: bold; -fx-background-radius: 7; -fx-cursor: hand;");
        Button confirm = new Button("Xác nhận thanh toán");
        confirm.setStyle("-fx-background-color: #B32638; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 7; -fx-cursor: hand;");

        Label result = new Label();
        result.setWrapText(true);
        result.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        cancel.setOnAction(event -> guiQuyetToan(auctionId, "CANCEL", cancel, confirm, result));
        confirm.setOnAction(event -> guiQuyetToan(auctionId, "CONFIRM", cancel, confirm, result));

        HBox actions = new HBox(10, cancel, confirm);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox card = taoKhungThongBao();
        card.getChildren().addAll(title, time, body, result, actions);
        return bocThongBao("✓", card);
    }

    private Label taoNhanThoiGian(String sentAt) {
        Label time = new Label(dinhDangThoiGian(sentAt));
        time.setStyle("-fx-text-fill: #8A6F66; -fx-font-size: 11px;");
        return time;
    }

    private String dinhDangThoiGian(String sentAt) {
        if (sentAt == null || sentAt.isBlank()) {
            return LocalDateTime.now().format(DISPLAY_TIME_FORMAT);
        }
        String normalized = sentAt.trim().replace('T', ' ');
        int dotIndex = normalized.indexOf('.');
        if (dotIndex > 0) {
            normalized = normalized.substring(0, dotIndex);
        }
        try {
            return LocalDateTime.parse(normalized, DB_TIME_FORMAT).format(DISPLAY_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
            return sentAt;
        }
    }

    private VBox taoKhungThongBao() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(12));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color: #FFFDFC; -fx-background-radius: 8; "
                + "-fx-border-color: #E8DDD5; -fx-border-radius: 8;");
        return card;
    }

    private Node bocThongBao(String icon, VBox content) {
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

    private void cauHinhDanhSachThongBao() {
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
                (javafx.collections.ListChangeListener<Node>) change -> capNhatTongThongBao()
        );
    }

    private void themThongBao(Node notification) {
        lvChatMessages.getItems().add(0, notification);
        capNhatTongThongBao();
    }

    private void capNhatTongThongBao() {
        int tong = lvChatMessages == null ? 0 : lvChatMessages.getItems().size();
        if (lblNotificationCount != null) {
            lblNotificationCount.setText(tong + " thông báo");
        }
        if (emptyNotificationState != null) {
            emptyNotificationState.setManaged(tong == 0);
            emptyNotificationState.setVisible(tong == 0);
        }
    }

    private void taiThongBaoDaLuu() {
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
                for (int i = notifications.size() - 1; i >= 0; i--) {
                    hienThiThongBao(notifications.get(i).getAsJsonObject());
                }
            });
        });
    }

    private void danhDauThongBaoDaDoc() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.MARK_SYSTEM_NOTIFICATIONS_READ);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());
        ClientSocket.getInstance().sendJsonRequest(request, "MARK_NOTIFICATIONS_READ_RESPONSE", null);
    }

    private void guiQuyetToan(int auctionId, String decision, Button cancel, Button confirm, Label result) {
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
                    hienThiKetQua(message);
                }
            });
        });
    }

    private void hienThiKetQua(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Quyết toán phiên đấu giá");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void shutdown() {
        instance = null;
    }
}
