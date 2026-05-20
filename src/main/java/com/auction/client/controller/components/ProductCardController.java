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
    private String currentStatus = "";

    /**
     * Hàm cấu hình dữ liệu chuẩn của Card Sản phẩm
     */
    public void setProductData(int auctionId, String name, double price, int countdownSeconds, String status, String imageUrl, boolean isFollowed) {
        this.auctionId = auctionId;
        this.imageUrl = imageUrl;
        this.isFollowed = isFollowed;
        this.currentStatus = status;
        this.timeInSeconds = countdownSeconds;

        // DỌN DẸP SẠCH ĐỒNG HỒ CŨ TRƯỚC KHI NẠP DỮ LIỆU MỚI (Tránh rò rỉ luồng chạy ngầm)
        stopTimer();

        lblProductName.setText(name);
        lblCurrentPrice.setText(String.format("%,.0f đ", price));
        lblTimeRemaining.setText(formatTime(timeInSeconds));

        if (imgProduct != null) {
            try {
                imgProduct.setImage(com.auction.client.util.ImageCacheManager.getImage(imageUrl));
            } catch (Exception e) {
                imgProduct.setImage(null);
            }
        }

        updateHeartUI();

        // ĐỒNG BỘ TUYỆT ĐỐI THEO TRẠNG THÁI GỐC TỪ DATABASE
        if ("OPEN".equalsIgnoreCase(status)) {
            lblStatus.setText("Sắp diễn ra");
            lblStatus.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-weight: bold;");
            lblTimeRemaining.setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold;");
            btnBid.setDisable(true);
            btnBid.setText("Chờ mở bán");
            if (timeInSeconds > 0) startCountdown();

        } else if ("RUNNING".equalsIgnoreCase(status)) {
            lblStatus.setText("Đang diễn ra");
            lblStatus.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-weight: bold;");
            lblTimeRemaining.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
            btnBid.setDisable(false);
            btnBid.setText("Vào phòng");
            if (timeInSeconds > 0) startCountdown();

        } else {
            setExpiredUI();
        }
    }

    public void setProductData(int auctionId, String name, double price, int countdownSeconds, String status, String imageUrl) {
        setProductData(auctionId, name, price, countdownSeconds, status, imageUrl, false);
    }

    private void updateHeartUI() {
        if (btnFollow == null) return;
        if (isFollowed) {
            btnFollow.setText("♥");
            btnFollow.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #e74c3c; -fx-padding: 0; -fx-font-size: 16px;");
        } else {
            btnFollow.setText("♡");
            btnFollow.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #888888; -fx-padding: 0; -fx-font-size: 16px;");
        }
    }

    private void startCountdown() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (timeInSeconds > 0) {
                timeInSeconds--;
                lblTimeRemaining.setText(formatTime(timeInSeconds));

                // Sửa lỗi nhấp nháy: Đảm bảo giữ màu đỏ đậm ổn định nếu dưới 30s, ngược lại giữ màu xanh lá
                if ("RUNNING".equalsIgnoreCase(currentStatus)) {
                    if (timeInSeconds <= 30) {
                        lblTimeRemaining.setStyle("-fx-text-fill: #A64452; -fx-font-weight: bold;");
                    } else {
                        lblTimeRemaining.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    }
                }
            } else {
                stopTimer();

                if ("OPEN".equalsIgnoreCase(currentStatus)) {
                    System.out.println("🚀 Card [" + lblProductName.getText() + "] chuyển trạng thái: OPEN -> RUNNING!");
                    // Giả định phiên chạy thực tế mở trong 20 phút (1200 giây) ngoài sảnh chính
                    setProductData(auctionId, lblProductName.getText(), Double.parseDouble(lblCurrentPrice.getText().replaceAll("\\D", "")), 1200, "RUNNING", imageUrl, isFollowed);
                } else {
                    setExpiredUI();
                }
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void setExpiredUI() {
        lblTimeRemaining.setText("00:00:00");
        lblTimeRemaining.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        lblStatus.setText("Đã kết thúc");
        lblStatus.setStyle("-fx-background-color: #F5F5F5; -fx-text-fill: #888888; -fx-padding: 2 8; -fx-background-radius: 4;");
        btnBid.setDisable(true);
        btnBid.setText("Hết hạn");
    }

    /**
     * Hàm dọn dẹp bộ nhớ đếm ngược (Bắt buộc phải gọi khi hủy hoặc làm mới sảnh)
     */
    public void stopTimer() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    @FXML
    private void handleBidAction(ActionEvent event) {
        if (auctionId == -1) {
            showAlert("Cảnh báo", "Không tìm thấy dữ liệu phòng đấu giá này!");
            return;
        }

        if ("OPEN".equalsIgnoreCase(currentStatus)) {
            showAlert("Thông báo", "Phiên đấu giá chưa mở, vui lòng đợi đếm ngược mở phòng!");
            return;
        }

        // BÍ KÍP ĐỒNG BỘ: Trước khi chuyển cảnh, dọn dẹp sạch Timer của Card hiện tại tránh chạy ngầm vô dụng
        stopTimer();

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
                if (!(response.has("success") && response.get("success").getAsBoolean())) {
                    isFollowed = !isFollowed;
                    updateHeartUI();
                }
            });
        });
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
}