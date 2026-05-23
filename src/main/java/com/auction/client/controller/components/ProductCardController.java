package com.auction.client.controller.components;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonObject;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductCardController {

    private static final Logger logger = LoggerFactory.getLogger(ProductCardController.class);

    @FXML private ImageView imgProduct;
    @FXML private Label lblProductName;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeRemaining;
    @FXML private Label lblStatus;
    @FXML private Button btnBid;
    @FXML private Button btnFollow;

    private long deadlineMillis;
    private Timeline timeline;
    private int auctionId = -1;
    private String imageUrl = "";
    private boolean isFollowed = false;
    private boolean followRequestPending = false;
    private String currentStatus = "";

    public void setProductData(int auctionId, String name, double price, long countdownSeconds, String status, String imageUrl, boolean isFollowed
    ) {
        stopTimer();

        this.auctionId = auctionId;
        this.imageUrl = imageUrl;
        this.isFollowed = isFollowed;
        this.currentStatus = status == null ? "FINISHED" : status;

        this.deadlineMillis = System.currentTimeMillis() + Math.max(0, countdownSeconds) * 1000L;

        if (lblProductName != null) {
            lblProductName.setText(name);
        }

        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(String.format("%,.0f đ", price));
        }

        loadImage();
        updateHeartUI();
        setupStatusUI(this.currentStatus);

        if (countdownSeconds > 0 && isTimedStatus(this.currentStatus)) {
            updateCountdown();
            startCountdown();
        } else if (!isTimedStatus(this.currentStatus)) {
            setClosedByStatus(this.currentStatus);
        } else {
            updateCountdown();
        }
    }
    public void setProductData(int auctionId, String name, double price, long countdownSeconds, String status, String imageUrl) {
        setProductData(auctionId, name, price, countdownSeconds, status, imageUrl, false);
    }

    private void loadImage() {
        if (imgProduct == null) return;

        try {
            imgProduct.setImage(com.auction.client.util.ImageCacheManager.getPreviewImage(imageUrl));
        } catch (Exception e) {
            logger.error("Lỗi load ảnh", e);
            imgProduct.setImage(null);
        }
    }

    private void setupStatusUI(String status) {
        if ("OPEN".equalsIgnoreCase(status)) {
            setOpenUI();

        } else if ("RUNNING".equalsIgnoreCase(status)) {
            setRunningUI();

        } else {
            setClosedByStatus(status);
        }
    }

    private void setOpenUI() {
        if (lblStatus != null) {
            lblStatus.setText("Sắp diễn ra");
            lblStatus.setStyle(
                    "-fx-background-color: #FFF3E0;" +
                            "-fx-text-fill: #E65100;" +
                            "-fx-padding: 2 8;" +
                            "-fx-background-radius: 4;" +
                            "-fx-font-weight: bold;"
            );
        }

        if (lblTimeRemaining != null) {
            lblTimeRemaining.setStyle("-fx-text-fill: #E65100;" + "-fx-font-weight: bold;");
        }

        if (btnBid != null) {
            btnBid.setDisable(true);
            btnBid.setText("Chờ mở bán");
        }
    }

    private void setRunningUI() {
        stopTimer();

        currentStatus = "RUNNING";

        if (lblStatus != null) {
            lblStatus.setText("Đang diễn ra");
            lblStatus.setStyle(
                    "-fx-background-color: #E8F5E9;" +
                            "-fx-text-fill: #2E7D32;" +
                            "-fx-padding: 2 8;" +
                            "-fx-background-radius: 4;" +
                            "-fx-font-weight: bold;"
            );
        }

        if (lblTimeRemaining != null) {
            lblTimeRemaining.setText("Đang mở");
            lblTimeRemaining.setStyle(
                    "-fx-text-fill: #2E7D32;" +
                            "-fx-font-weight: bold;"
            );
        }

        if (btnBid != null) {
            btnBid.setDisable(false);
            btnBid.setText("Vào phòng");
        }
    }

    private void setClosedByStatus(String status) {
        if ("PAID".equalsIgnoreCase(status)) {
            setClosedUI("Đã thanh toán", "Đã thanh toán");

        } else if ("CANCELED".equalsIgnoreCase(status)
                || "CANCELLED".equalsIgnoreCase(status)) {
            setClosedUI("Đã hủy", "Đã hủy");

        } else {
            setExpiredUI();
        }
    }

    private boolean isTimedStatus(String status) {
        return "OPEN".equalsIgnoreCase(status) || "RUNNING".equalsIgnoreCase(status);
    }

    private void startCountdown() {
        stopTimer();

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown()));

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void updateCountdown() {
        long now = System.currentTimeMillis();
        long remaining = (deadlineMillis - now) / 1000;

        if (remaining <= 0) {
            if ("OPEN".equalsIgnoreCase(currentStatus)) {
                setRunningUI();
            } else {
                setExpiredUI();
            }
            return;
        }

        if (lblTimeRemaining != null) {
            lblTimeRemaining.setText(formatTime((int) remaining));
        }

        if ("RUNNING".equalsIgnoreCase(currentStatus) && remaining <= 30 && lblTimeRemaining != null) {
            lblTimeRemaining.setStyle("-fx-text-fill: #A64452;" + "-fx-font-weight: bold;");
        }
    }

    private void setExpiredUI() {
        currentStatus = "FINISHED";
        setClosedUI("Đã kết thúc", "Hết hạn");
    }

    private void setClosedUI(String statusText, String buttonText) {
        stopTimer();

        if (lblTimeRemaining != null) {
            lblTimeRemaining.setText("00:00:00");
            lblTimeRemaining.setStyle(
                    "-fx-text-fill: red;" +
                            "-fx-font-weight: bold;"
            );
        }

        if (lblStatus != null) {
            lblStatus.setText(statusText);
            lblStatus.setStyle(
                    "-fx-background-color: #F5F5F5;" +
                            "-fx-text-fill: #888888;" +
                            "-fx-padding: 2 8;" +
                            "-fx-background-radius: 4;"
            );
        }

        if (btnBid != null) {
            btnBid.setDisable(true);
            btnBid.setText(buttonText);
        }
    }

    public void stopTimer() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    private void updateHeartUI() {
        if (btnFollow == null) return;

        if (isFollowed) {
            btnFollow.setText("♥");
            btnFollow.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-cursor: hand;" +
                            "-fx-text-fill: #e74c3c;" +
                            "-fx-padding: 0;" +
                            "-fx-font-size: 16px;"
            );

        } else {
            btnFollow.setText("♡");
            btnFollow.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-cursor: hand;" +
                            "-fx-text-fill: #888888;" +
                            "-fx-padding: 0;" +
                            "-fx-font-size: 16px;"
            );
        }
    }

    @FXML
    private void handleBidAction(ActionEvent event) {
        if (auctionId == -1) {
            showAlert("Cảnh báo", "Không tìm thấy dữ liệu phòng đấu giá!");
            return;
        }

        if ("OPEN".equalsIgnoreCase(currentStatus)) {
            showAlert("Thông báo", "Phiên đấu giá chưa mở!");
            return;
        }

        if ("FINISHED".equalsIgnoreCase(currentStatus) || "PAID".equalsIgnoreCase(currentStatus) || "CANCELED".equalsIgnoreCase(currentStatus) || "CANCELLED".equalsIgnoreCase(currentStatus)) {
            showAlert("Thông báo", "Phiên đấu giá không còn hoạt động!");
            return;
        }

        stopTimer();

        com.auction.client.controller.MainController.instance.setCenterContent("/fxml/bidder/AuctionRoom.fxml");

        Object controller = com.auction.client.controller.MainController.instance.getCurrentCenterController();

        if (controller instanceof
                com.auction.client.controller.bidder.AuctionRoomController room) {
            room.initData(auctionId, imageUrl);
        }
    }

    @FXML
    private void handleFollowAction(ActionEvent event) {
        if (auctionId == -1 || followRequestPending) return;

        boolean previousFollowState = isFollowed;

        isFollowed = !isFollowed;
        followRequestPending = true;

        updateHeartUI();

        if (btnFollow != null) {
            btnFollow.setDisable(true);
        }

        JsonObject req = new JsonObject();

        req.addProperty(
                "type",
                isFollowed
                        ? ActionType.FOLLOW_AUCTION
                        : ActionType.UNFOLLOW_AUCTION
        );

        req.addProperty("auctionId", auctionId);
        req.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(
                req,
                null,
                response -> Platform.runLater(() -> {
                    followRequestPending = false;

                    if (btnFollow != null) {
                        btnFollow.setDisable(false);
                    }

                    if (response.has("isFollowed") && !response.get("isFollowed").isJsonNull()) {
                        isFollowed = response.get("isFollowed").getAsBoolean();

                    } else if (!(response.has("success") && response.get("success").getAsBoolean())) {
                        isFollowed = previousFollowState;
                    }

                    updateHeartUI();
                })
        );
    }

    private String formatTime(int totalSeconds) {
        totalSeconds = Math.max(totalSeconds, 0);

        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void showAlert(String title, String content) {
        Alert alert =
                new Alert(Alert.AlertType.WARNING);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void initialize() {
        if (lblProductName != null) {
            lblProductName.sceneProperty().addListener(
                    (obs, oldScene, newScene) -> {
                        if (newScene == null) {
                            stopTimer();
                        }
                    }
            );
        }
    }
}