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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class AuctionRoomController implements Initializable {

    @FXML private Label lblCurrentPrice, lblCountdown, lblLeader;
    @FXML private ListView<String> lvBidHistory;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;

    // 🔥 BIẾN UI BỔ SUNG: Ô nhập mức giá tối đa cho Auto Bid và Nút kích hoạt
    @FXML private TextField txtMaxAutoBid;
    @FXML private Button btnEnableAutoBid;

    // 🔥 BIẾN UI BỔ SUNG: ImageView nhận hiển thị ảnh sản phẩm từ URL
    @FXML private ImageView imgProduct;

    private int totalSeconds = 900;
    private Timeline countdownTimeline;
    private final int currentAuctionId = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Đăng ký nhận luồng Real-time từ Server (Observer Push)
        com.auction.client.network.PushHandler.currentRoomController = this;

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

        // Bắt lỗi nhập liệu cho ô Auto Bid (Chỉ cho nhập số)
        if (txtMaxAutoBid != null) {
            txtMaxAutoBid.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.isEmpty()) {
                    txtMaxAutoBid.setText(newVal.replaceAll("\\D", ""));
                }
            });
        }

        // 🔥 DEMO LOAD ẢNH BAN ĐẦU (Tối ráp Socket sẽ truyền biến động url từ Server của Kiên trả về vào đây)
        loadProductImage("");
    }

    private void startCountdown() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            totalSeconds--;

            if (totalSeconds <= 0) {
                lblCountdown.setText("HẾT GIỜ!");
                lblCountdown.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                btnPlaceBid.setDisable(true);
                if (btnEnableAutoBid != null) btnEnableAutoBid.setDisable(true);
                txtBidAmount.setEditable(false);
                countdownTimeline.stop();
            } else {
                updateCountdownLabel();

                // 🔥 ĐỔI MÀU ĐỒNG HỒ CẢNH BÁO: Nếu thời gian dưới 30 giây, chuyển chữ sang màu đỏ nhấp nháy cho kịch tính
                if (totalSeconds <= 30) {
                    lblCountdown.setStyle("-fx-text-fill: #A64452; -fx-font-weight: bold;");
                }
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
            int bidderId = 1; // Tạm thời mock ID người mua bằng 1

            // Khởi tạo DTO đặt giá chuẩn của Kiên
            AuctionDTOs.BidRequest request = new AuctionDTOs.BidRequest(currentAuctionId, bidAmount, bidderId);

            btnPlaceBid.setDisable(true);

            // Tối ưu cấu trúc map sang JsonObject
            JsonObject jsonRequest = new Gson().toJsonTree(request).getAsJsonObject();

            // 🔥 SỬA CHO ĐÚNG THEO SERVER: Đổi từ "PLACE_BID" thành "BID_REQUEST"
            // để khớp chuẩn 100% với tên hành động định nghĩa trong file AuctionDTOs.java ở Common!
            jsonRequest.addProperty("type", "BID_REQUEST");

            ClientSocket.getInstance().sendJsonRequest(jsonRequest, "BID_RESPONSE", response -> {
                // Đảm bảo cập nhật UI trên luồng JavaFX (Thread-safety)
                Platform.runLater(() -> {
                    btnPlaceBid.setDisable(false);
                    boolean success = response.has("success") && response.get("success").getAsBoolean();
                    if (!success) {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Đã có người trả giá cao hơn.";
                        showAlert("Giá không hợp lệ", msg);
                    } else {
                        // Nếu đặt giá thành công cục bộ, kiểm tra xem có cần tự kích hoạt luật gia hạn 1 phút không (Đề phòng mạng chậm)
                        checkAndApplySnipingRule();
                    }
                });
            });

            txtBidAmount.clear();

        } catch (NumberFormatException e) {
            showAlert("Lỗi nhập liệu", "Vui lòng chỉ nhập số hợp lệ!");
        }
    }

    // =========================================================================
    // 🔥 CHỨC NĂNG TỰ ĐỘNG ĐẤU GIÁ (AUTO BIDDING)
    // =========================================================================
    @FXML
    private void handleEnableAutoBid() {
        if (txtMaxAutoBid == null || txtMaxAutoBid.getText().trim().isEmpty()) {
            showAlert("Thiếu thông tin", "Vui lòng nhập số tiền tối đa bạn có thể trả cho sản phẩm này!");
            return;
        }

        try {
            long maxPrice = Long.parseLong(txtMaxAutoBid.getText().trim());
            int bidderId = 1;

            // Đóng gói JSON gửi yêu cầu đăng ký Auto Bid lên Server
            JsonObject jsonRequest = new JsonObject();
            jsonRequest.addProperty("type", "ENABLE_AUTO_BID");
            jsonRequest.addProperty("auctionId", currentAuctionId);
            jsonRequest.addProperty("bidderId", bidderId);
            jsonRequest.addProperty("maxPrice", maxPrice);

            if (btnEnableAutoBid != null) btnEnableAutoBid.setDisable(true);

            ClientSocket.getInstance().sendJsonRequest(jsonRequest, "AUTO_BID_RESPONSE", response -> {
                Platform.runLater(() -> {
                    if (btnEnableAutoBid != null) btnEnableAutoBid.setDisable(false);
                    boolean success = response.has("success") && response.get("success").getAsBoolean();
                    if (success) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Thành công");
                        alert.setHeaderText(null);
                        alert.setContentText("Đã kích hoạt hệ thống Tự động đấu giá (Auto Bidding) thành công! Hệ thống sẽ tự nâng giá để bảo vệ vị thế dẫn đầu của bạn.");
                        alert.showAndWait();
                    } else {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi không thể kích hoạt.";
                        showAlert("Thất bại", msg);
                    }
                });
            });

        } catch (NumberFormatException e) {
            showAlert("Lỗi nhập liệu", "Giá trần Auto Bid không hợp lệ!");
        }
    }

    // =========================================================================
    // GIA HẠN THÊM 1 PHÚT (ANTI-SNIPING RULE)
    // =========================================================================
    private void checkAndApplySnipingRule() {
        // Luật đặt ra: Nếu thời gian còn lại ít hơn hoặc bằng 30 giây mà có người bid, tự động cộng thêm 60 giây
        if (totalSeconds > 0 && totalSeconds <= 30) {
            totalSeconds += 60;
            // Khôi phục lại màu sắc bình thường cho đồng hồ
            lblCountdown.setStyle("-fx-text-fill: #1A0F0A; -fx-font-weight: normal;");
            updateCountdownLabel();
            org.slf4j.LoggerFactory.getLogger(getClass()).info("[Anti-Sniping] Phát hiện bid trong 30s cuối! Tự động gia hạn thêm 1 phút.");
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

        // 🔥 ĐỒNG BỘ REALTIME: Bất kể ai đặt giá (hoặc hệ thống tự động Auto Bid của người khác kích nổ) trong 30s cuối,
        // toàn bộ các máy Client đang xem phòng này đều sẽ được tự động gia hạn thêm 1 phút đồng bộ cùng nhau!
        checkAndApplySnipingRule();
    }

    // =========================================================================
    // 🔥LOGIC HIỂN THỊ ẢNH SẢN PHẨM TỪ ĐƯỜNG DẪN URL
    // =========================================================================
    public void loadProductImage(String urlString) {
        if (imgProduct == null) return;

        if (urlString == null || urlString.trim().isEmpty()) {
            // Nếu không truyền URL, mặc định clear trống ImageView để lộ icon xe FXML nền bên dưới
            imgProduct.setImage(null);
            return;
        }

        try {
            // Nạp ảnh qua luồng ngầm (backgroundLoading = true) để chống đơ lag giao diện Client
            Image image = new Image(urlString, true);

            // Gài Listener bẫy lỗi nếu link URL die hoặc sai định dạng file ảnh
            image.exceptionProperty().addListener((obs, oldExc, newExc) -> {
                if (newExc != null) {
                    org.slf4j.LoggerFactory.getLogger(getClass()).error("[UI Error] Không thể nạp ảnh sản phẩm từ URL: {}", newExc.getMessage());
                    Platform.runLater(() -> imgProduct.setImage(null)); // Xóa ảnh lỗi để quay về icon mặc định
                }
            });

            imgProduct.setImage(image);
        } catch (Exception e) {
            imgProduct.setImage(null);
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