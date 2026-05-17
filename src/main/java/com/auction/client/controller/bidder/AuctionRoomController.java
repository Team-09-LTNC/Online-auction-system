package com.auction.client.controller.bidder;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.dto.AuctionDTOs;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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

    private int totalSeconds = 900;
    private Timeline countdownTimeline;
    private final int currentAuctionId = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Đăng ký nhận luồng Real-time từ Server (Observer Push)
        com.auction.client.networkclient.PushHandler.currentRoomController = this;

        lvBidHistory.getItems().addAll(
                "Mạnh Hùng: 3,420,000,000 đ",
                "Quốc Khánh: 3,480,000,000 đ",
                "Nguyễn An: 3,500,000,000 đ"
        );

        startCountdown();

        txtBidAmount.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                String cleanString = newVal.replaceAll("\\D", "");
                try {
                    Double.parseDouble(cleanString);
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    private void startCountdown() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            totalSeconds--;

            if (totalSeconds <= 0) {
                lblCountdown.setText("HẾT GIỜ!");
                lblCountdown.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                btnPlaceBid.setDisable(true);
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
            long bidAmount = Long.parseLong(input.replaceAll("\\D", ""));
            int bidderId = 1;

            AuctionDTOs.BidRequest request = new AuctionDTOs.BidRequest(currentAuctionId, bidAmount, bidderId);

            btnPlaceBid.setDisable(true);

            // [TỐI ƯU KIẾN TRÚC] Chuyển đổi sang JsonObject để gọi hàm mạng an toàn, đăng ký Callback
            JsonObject jsonRequest = new Gson().toJsonTree(request).getAsJsonObject();

            ClientSocket.getInstance().sendJsonRequest(jsonRequest, "BID_RESPONSE", response -> {
                // Đảm bảo cập nhật UI trên luồng JavaFX (Thread-safety)
                Platform.runLater(() -> {
                    btnPlaceBid.setDisable(false);
                    boolean success = response.has("success") && response.get("success").getAsBoolean();
                    if (!success) {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Đã có người trả giá cao hơn.";
                        showAlert("Giá không hợp lệ", msg);
                    }
                });
            });

            txtBidAmount.clear();

        } catch (NumberFormatException e) {
            showAlert("Lỗi nhập liệu", "Vui lòng chỉ nhập số hợp lệ!");
        }
    }

    /**
     * HÀM REAL-TIME: Gọi bởi PushHandler.
     * Lưu ý: Hàm này an toàn vì PushHandler đã bọc Platform.runLater().
     */
    public void updateRealtimeBid(long newPrice, String bidderName) {
        lblCurrentPrice.setText(String.format("%,.0f đ", (double) newPrice));
        lblLeader.setText("Người dẫn đầu: " + bidderName);

        // [TỐI ƯU JAVAFX] Sử dụng add(0, item) thay vì addFirst() để tương thích mọi phiên bản JDK
        String historyEntry = String.format("%s: %,.0f đ", bidderName, (double) newPrice);
        lvBidHistory.getItems().add(0, historyEntry);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}