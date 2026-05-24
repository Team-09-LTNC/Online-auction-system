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
import javafx.scene.layout.HBox;
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
    @FXML private HBox sellerActions;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    private long deadlineMillis;
    private Timeline timeline;
    private int auctionId = -1;
    private String imageUrl = "";
    private boolean isFollowed = false;
    private boolean followRequestPending = false;
    private String currentStatus = "";
    private JsonObject auctionSnapshot;
    private Runnable editAction;
    private Runnable deleteAction;

    public void setProductData(
            int auctionId,
            String name,
            double price,
            long countdownSeconds,
            String status,
            String imageUrl,
            boolean isFollowed
    ) {
        stopTimer();

        this.auctionId = auctionId;
        this.imageUrl = imageUrl;
        this.isFollowed = isFollowed;
        this.currentStatus = status;
        this.deadlineMillis = System.currentTimeMillis() + Math.max(0, countdownSeconds) * 1000L;
        this.auctionSnapshot = createBasicSnapshot(auctionId, name, price, countdownSeconds, status, imageUrl);

        lblProductName.setText(name);
        lblCurrentPrice.setText(String.format("%,.0f đ", price));

        loadImage();
        updateHeartUI();
        setupStatusUI(status);

        if (countdownSeconds > 0 && isTimedStatus(status)) {
            updateCountdown();
            startCountdown();
        } else {
            lblTimeRemaining.setText("00:00:00");
        }
    }

    public void setProductData(
            int auctionId,
            String name,
            double price,
            long countdownSeconds,
            String status,
            String imageUrl
    ) {
        setProductData(auctionId, name, price, countdownSeconds, status, imageUrl, false);
    }

    public void configureSellerActions(boolean editable, Runnable editAction, Runnable deleteAction) {
        this.editAction = editable ? editAction : null;
        this.deleteAction = editable ? deleteAction : null;

        if (btnFollow != null) {
            btnFollow.setVisible(false);
            btnFollow.setManaged(false);
        }

        if (sellerActions != null) {
            sellerActions.setVisible(editable);
            sellerActions.setManaged(editable);
        }
    }

    public void setAuctionSnapshot(JsonObject auctionSnapshot) {
        this.auctionSnapshot = auctionSnapshot == null ? null : auctionSnapshot.deepCopy();
    }

    private void loadImage() {
        if (imgProduct == null) return;
        try {
            imgProduct.setImage(
                    com.auction.client.util.ImageCacheManager.getPreviewImage(imageUrl)
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
        } else if ("PAID".equalsIgnoreCase(status)) {
            setClosedUI("Đã thanh toán", "Đã thanh toán");
        } else if ("CANCELED".equalsIgnoreCase(status)) {
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

        lblTimeRemaining.setText(formatTime((int) remaining));

        if ("RUNNING".equalsIgnoreCase(currentStatus) && remaining <= 30) {
            lblTimeRemaining.setStyle("-fx-text-fill: #A64452; -fx-font-weight: bold;");
        }
    }

    private void setExpiredUI() {
        currentStatus = "FINISHED";
        setClosedUI("Đã kết thúc", "Hết hạn");
    }

    private void setRunningUI() {
        stopTimer();
        currentStatus = "RUNNING";
        lblTimeRemaining.setText("Đang mở");
        lblStatus.setText("Đang diễn ra");
        btnBid.setDisable(false);
        btnBid.setText("Vào phòng");
        hideSellerActions();
    }

    private void setClosedUI(String statusText, String buttonText) {
        stopTimer();

        lblTimeRemaining.setText("00:00:00");
        lblStatus.setText(statusText);

        btnBid.setDisable(true);
        btnBid.setText(buttonText);
        hideSellerActions();
    }

    private void hideSellerActions() {
        editAction = null;
        deleteAction = null;
        if (sellerActions != null) {
            sellerActions.setVisible(false);
            sellerActions.setManaged(false);
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
            room.initData(auctionId, imageUrl, auctionSnapshot);
        }
    }

    @FXML
    private void handleFollowAction(ActionEvent event) {
        if (auctionId == -1 || followRequestPending) return;

        boolean previousFollowState = isFollowed;
        isFollowed = !isFollowed;
        followRequestPending = true;
        updateHeartUI();
        btnFollow.setDisable(true);

        JsonObject req = new JsonObject();
        req.addProperty("type",
                isFollowed ? ActionType.FOLLOW_AUCTION : ActionType.UNFOLLOW_AUCTION);
        req.addProperty("auctionId", auctionId);
        req.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(
                req,
                null,
                response -> Platform.runLater(() -> {
                    followRequestPending = false;
                    btnFollow.setDisable(false);

                    if (response.has("isFollowed") && !response.get("isFollowed").isJsonNull()) {
                        isFollowed = response.get("isFollowed").getAsBoolean();
                    } else if (!(response.has("success") && response.get("success").getAsBoolean())) {
                        isFollowed = previousFollowState;
                    }
                    updateHeartUI();
                })
        );
    }

    @FXML
    private void handleEditAction(ActionEvent event) {
        if (editAction != null) {
            editAction.run();
        }
    }

    @FXML
    private void handleDeleteAction(ActionEvent event) {
        if (deleteAction != null) {
            deleteAction.run();
        }
    }

    private String formatTime(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private JsonObject createBasicSnapshot(
            int auctionId,
            String name,
            double price,
            long countdownSeconds,
            String status,
            String imageUrl
    ) {
        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("auctionId", auctionId);
        snapshot.addProperty("itemName", name);
        snapshot.addProperty("currentPrice", (long) price);
        snapshot.addProperty("currentHighestBid", (long) price);
        snapshot.addProperty("countdownSeconds", countdownSeconds);
        snapshot.addProperty("status", status);
        snapshot.addProperty("imageUrl", imageUrl);
        return snapshot;
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
        if (sellerActions != null) {
            sellerActions.setVisible(false);
            sellerActions.setManaged(false);
        }

        if (lblProductName != null) {
            lblProductName.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) stopTimer();
            });
        }
    }
}
