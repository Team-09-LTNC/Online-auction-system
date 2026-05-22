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

    private long endTimeMillis;
    private Timeline timeline;
    private int auctionId = -1;
    private String imageUrl = "";
    private boolean isFollowed = false;
    private String currentStatus = "";

    public void setProductData(
            int auctionId,
            String name,
            double price,
            long endTimeMillis,
            String status,
            String imageUrl,
            boolean isFollowed
    ) {
        stopTimer();

        this.auctionId = auctionId;
        this.imageUrl = imageUrl;
        this.isFollowed = isFollowed;
        this.currentStatus = status;
        this.endTimeMillis = endTimeMillis;

        lblProductName.setText(name);
        lblCurrentPrice.setText(String.format("%,.0f đ", price));

        loadImage();
        updateHeartUI();
        setupStatusUI(status);

        updateCountdown();

        if (endTimeMillis > System.currentTimeMillis()
                && !"FINISHED".equalsIgnoreCase(status)) {
            startCountdown();
        }
    }

    public void setProductData(
            int auctionId,
            String name,
            double price,
            long endTimeMillis,
            String status,
            String imageUrl
    ) {
        setProductData(auctionId, name, price, endTimeMillis, status, imageUrl, false);
    }

    private void loadImage() {
        if (imgProduct == null) return;
        try {
            imgProduct.setImage(
                    com.auction.client.util.ImageCacheManager.getImage(imageUrl)
            );
        } catch (Exception e) {
            logger.error("Lỗi load ảnh", e);
            imgProduct.setImage(null);
        }
    }

    private void setupStatusUI(String status) {
        if ("OPEN".equalsIgnoreCase(status)) {
            lblStatus.setText("Sắp diễn ra");
            btnBid.setDisable(true);
            btnBid.setText("Chờ mở bán");
        } else if ("RUNNING".equalsIgnoreCase(status)) {
            lblStatus.setText("Đang diễn ra");
            btnBid.setDisable(false);
            btnBid.setText("Vào phòng");
        } else {
            setExpiredUI();
        }
    }

    private void startCountdown() {
        stopTimer();

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void updateCountdown() {
        long now = System.currentTimeMillis();
        long remaining = (endTimeMillis - now) / 1000;

        if (remaining <= 0) {
            setExpiredUI();
            return;
        }

        lblTimeRemaining.setText(formatTime((int) remaining));

        if ("RUNNING".equalsIgnoreCase(currentStatus) && remaining <= 30) {
            lblTimeRemaining.setStyle("-fx-text-fill: #A64452; -fx-font-weight: bold;");
        }
    }

    private void setExpiredUI() {
        stopTimer();
        currentStatus = "FINISHED";

        lblTimeRemaining.setText("00:00:00");
        lblStatus.setText("Đã kết thúc");

        btnBid.setDisable(true);
        btnBid.setText("Hết hạn");
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
            btnFollow.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #FF0000; -fx-padding: 0;");
        } else {
            btnFollow.setText("♡");
            btnFollow.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #888888; -fx-padding: 0;");
        }
    }

    @FXML
    private void handleBidAction(ActionEvent event) {
        if (auctionId == -1) return;

        if ("FINISHED".equalsIgnoreCase(currentStatus)) {
            showAlert("Thông báo", "Phiên đấu giá đã kết thúc!");
            return;
        }

        stopTimer();

        com.auction.client.controller.MainController.instance
                .setCenterContent("/fxml/bidder/AuctionRoom.fxml");

        Object controller = com.auction.client.controller.MainController.instance.getCurrentCenterController();

        if (controller instanceof com.auction.client.controller.bidder.AuctionRoomController room) {
            room.initData(auctionId, imageUrl);
        }
    }

    @FXML
    private void handleFollowAction(ActionEvent event) {
        if (auctionId == -1) return;

        isFollowed = !isFollowed;
        updateHeartUI();

        JsonObject req = new JsonObject();
        req.addProperty("type",
                isFollowed ? ActionType.FOLLOW_AUCTION : ActionType.UNFOLLOW_AUCTION);
        req.addProperty("auctionId", auctionId);

        ClientSocket.getInstance().sendJsonRequest(
                req,
                "FOLLOW_RESPONSE",
                response -> Platform.runLater(() -> {
                    if (!(response.has("success") && response.get("success").getAsBoolean())) {
                        isFollowed = !isFollowed;
                        updateHeartUI();
                    }
                })
        );
    }

    private String formatTime(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void initialize() {
        if (lblProductName != null) {
            lblProductName.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) stopTimer();
            });
        }
    }
}