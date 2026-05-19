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
import javafx.scene.image.Image;
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

    private int timeInSeconds;
    private Timeline timeline;
    private int auctionId = -1;
    private String imageUrl = "";
    private boolean isFollowed = false;

    public void setProductData(int auctionId, String name, double price, String time, String status, String imageUrl, boolean isFollowed) {
        this.auctionId = auctionId;
        this.imageUrl = imageUrl;
        this.isFollowed = isFollowed;

        lblProductName.setText(name);
        lblCurrentPrice.setText(String.format("%,.0f đ", price));
        lblTimeRemaining.setText(time);
        lblStatus.setText(status);

        // TẢI ẢNH
        if (imgProduct != null && imageUrl != null && !imageUrl.trim().isEmpty()) {
            try {
                // Tham số 'true' giúp tải ảnh ở luồng nền (Background Thread) không làm đơ UI
                Image image = new Image(imageUrl, true);
                imgProduct.setImage(image);
            } catch (Exception e) {
                logger.warn("Không thể tải ảnh cho sản phẩm ID {}: {}", auctionId, imageUrl);
            }
        }

        updateHeartUI();

        this.timeInSeconds = parseTimeToSeconds(time);
        if ("Đã kết thúc".equals(status) || timeInSeconds <= 0) {
            stopAndFinish();
        } else {
            startCountdown();
        }
    }

    public void setProductData(int auctionId, String name, double price, String time, String status, String imageUrl) {
        setProductData(auctionId, name, price, time, status, imageUrl, false);
    }


    private void updateHeartUI() {
        if (isFollowed) {
            btnFollow.setText("♥");
            btnFollow.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #e74c3c; -fx-padding: 0;");
        } else {
            btnFollow.setText("♡");
            btnFollow.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #888888; -fx-padding: 0;");
        }
    }

    private void startCountdown() {
        if (timeline != null) timeline.stop();

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (timeInSeconds > 0) {
                timeInSeconds--;
                lblTimeRemaining.setText(formatTime(timeInSeconds));
            } else {
                stopAndFinish();
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void stopAndFinish() {
        if (timeline != null) timeline.stop();
        lblTimeRemaining.setText("00:00:00");
        lblStatus.setText("Đã kết thúc");
        lblStatus.setStyle("-fx-text-fill: #757575;");
        btnBid.setDisable(true);
        btnBid.setText("Hết hạn");
    }

    @FXML
    private void handleBidAction(ActionEvent event) {
        logger.info("Người dùng muốn vào phòng đấu giá ID: {}", auctionId);

        if (auctionId == -1) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Không tìm thấy dữ liệu phòng đấu giá này!");
            alert.showAndWait();
            return;
        }

        com.auction.client.controller.MainController.instance.setCenterContent("/fxml/bidder/AuctionRoom.fxml");

        Object controller = com.auction.client.controller.MainController.instance.getCurrentCenterController();
        if (controller instanceof com.auction.client.controller.bidder.AuctionRoomController) {
            ((com.auction.client.controller.bidder.AuctionRoomController) controller).initData(auctionId, imageUrl);
        }
    }

    @FXML
    private void handleFollowAction(ActionEvent event) {
        if (auctionId == -1) return;

        isFollowed = !isFollowed;
        updateHeartUI();
        btnFollow.setDisable(true);

        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("type", isFollowed ? ActionType.FOLLOW_AUCTION : ActionType.UNFOLLOW_AUCTION);
        jsonRequest.addProperty("auctionId", auctionId);

        ClientSocket.getInstance().sendJsonRequest(jsonRequest, isFollowed ? "FOLLOW_RESPONSE" : "UNFOLLOW_RESPONSE", response -> {
            Platform.runLater(() -> {
                btnFollow.setDisable(false);
                boolean success = response.has("success") && response.get("success").getAsBoolean();
                if (!success) {
                    isFollowed = !isFollowed;
                    updateHeartUI();
                }
            });
        });
    }

    private int parseTimeToSeconds(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
        } catch (Exception e) { return 0; }
    }

    private String formatTime(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}