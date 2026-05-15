package com.auction.client.controller.bidder;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Duration;
import java.net.URL;
import java.util.ResourceBundle;

public class AuctionRoomController implements Initializable {

    @FXML private Label lblCurrentPrice, lblCountdown, lblLeader;
    @FXML private ListView<String> lvBidHistory;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;

    private double currentPrice = 3500000000.0; // Giá hiện tại
    private int totalSeconds = 900; // Giả sử phiên còn 15 phút (900 giây)
    private Timeline countdownTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Nạp lịch sử ban đầu
        lvBidHistory.getItems().addAll(
                "Mạnh Hùng: 3,420,000,000 đ",
                "Quốc Khánh: 3,480,000,000 đ",
                "Nguyễn An: 3,500,000,000 đ"
        );

        // 2. Chạy đồng hồ đếm ngược
        startCountdown();

        // 3. Logic bổ sung: Format tiền khi đang gõ (Tăng trải nghiệm người dùng)
        txtBidAmount.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                String cleanString = newVal.replaceAll("[^\\d]", "");
                try {
                    double val = Double.parseDouble(cleanString);
                    // Cái này để m nhìn console xem số thật, không cần hiện lên UI để tránh rối
                    System.out.println("Giá trị đang nhập: " + val);
                } catch (NumberFormatException e) {}
            }
        });
    }

    private void startCountdown() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            totalSeconds--;

            if (totalSeconds <= 0) {
                lblCountdown.setText("HẾT GIỜ!");
                lblCountdown.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                btnPlaceBid.setDisable(true); // Khóa nút khi hết giờ
                txtBidAmount.setEditable(false);
                countdownTimeline.stop();
            } else {
                updateCountdownLabel();
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdownLabel() {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        lblCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));
    }

    @FXML
    private void handlePlaceBid() {
        String input = txtBidAmount.getText().trim();
        if (input.isEmpty()) return;

        try {
            // Loại bỏ các dấu phân cách nếu có trước khi parse
            double bidAmount = Double.parseDouble(input.replaceAll("[^\\d]", ""));

            // Logic: Giá mới phải cao hơn giá cũ (Ví dụ: bước giá tối thiểu là 10tr)
            if (bidAmount >= currentPrice + 10000000) {
                currentPrice = bidAmount;

                // Cập nhật giao diện
                lblCurrentPrice.setText(String.format("%,.0f đ", currentPrice));
                lblLeader.setText("Người dẫn đầu: Bạn");

                // Thêm vào đầu danh sách lịch sử
                lvBidHistory.getItems().add(0, "Bạn: " + String.format("%,.0f đ", bidAmount));
                txtBidAmount.clear();

                System.out.println("[AuctionRoom] Đặt giá thành công: " + bidAmount);
            } else {
                showAlert("Giá không hợp lệ", "M phải trả cao hơn ít nhất 10,000,000 đ so với giá hiện tại!");
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi nhập liệu", "Vui lòng chỉ nhập số!");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}