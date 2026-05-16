package com.auction.client.controller.components;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductCardController {
    // Logger để in log ra console cho dễ debug
    private static final Logger logger = LoggerFactory.getLogger(ProductCardController.class);

    // --- CÁC BIẾN LIÊN KẾT VỚI FXML (fx:id) ---
    @FXML private Label lblProductName;     // Tên sản phẩm
    @FXML private Label lblCurrentPrice;    // Giá hiện tại
    @FXML private Label lblTimeRemaining;   // Thời gian còn lại
    @FXML private Label lblStatus;          // Trạng thái (Đang diễn ra/Kết thúc)
    @FXML private Button btnBid;            // Nút "Ra giá"

    // --- BIẾN PHỤC VỤ ĐẾM NGƯỢC ---
    private int timeInSeconds;
    private Timeline timeline;

    /**
     * Hàm đổ dữ liệu vào Card.
     * Được gọi từ vòng lặp ở trang chủ (MainDashboardController).
     */
    public void setProductData(String name, double price, String time, String status) {
        // Gán tên sản phẩm
        lblProductName.setText(name);

        // Định dạng giá tiền có dấu phẩy (Ví dụ: 3,500,000,000)
        lblCurrentPrice.setText(String.format("%,.0f đ", price));

        // Gán thời gian và trạng thái
        lblTimeRemaining.setText(time);
        lblStatus.setText(status);

        // --- XỬ LÝ ĐẾM NGƯỢC ---
        this.timeInSeconds = parseTimeToSeconds(time);

        if ("Đã kết thúc".equals(status) || timeInSeconds <= 0) {
            stopAndFinish();
        } else {
            startCountdown();
        }
    }

    /**
     * Chạy đồng hồ đếm ngược từng giây
     */
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

    /**
     * Dừng đếm ngược và cập nhật trạng thái kết thúc
     */
    private void stopAndFinish() {
        if (timeline != null) timeline.stop();
        lblTimeRemaining.setText("00:00:00");
        lblStatus.setText("Đã kết thúc");
        lblStatus.setStyle("-fx-text-fill: #757575;"); // Màu xám
        btnBid.setDisable(true); // Vô hiệu hóa nút
        btnBid.setText("Hết hạn");
    }

    /**
     * Hàm xử lý khi người dùng bấm nút "Ra giá" trên Card.
     * (onAction="#handleBidAction" bên FXML)
     */
    @FXML
    private void handleBidAction(ActionEvent event) {
        String name = lblProductName.getText();
        logger.info("Người dùng muốn vào phòng đấu giá sản phẩm: {}", name);

        // Gọi MainController để thay đổi vùng nội dung chính (Center)
        com.auction.client.controller.MainController.instance.setCenterContent("/fxml/bidder/AuctionRoom.fxml");
    }

    // Chuyển HH:mm:ss -> tổng số giây
    private int parseTimeToSeconds(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
        } catch (Exception e) { return 0; }
    }

    // Chuyển giây -> HH:mm:ss
    private String formatTime(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}