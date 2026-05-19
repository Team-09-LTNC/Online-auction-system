package com.auction.client.controller.bidder;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.dto.AuctionDTOs;
import com.auction.common.enums.ActionType;
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

    @FXML private TextField txtMaxAutoBid;
    @FXML private Button btnEnableAutoBid;

    @FXML private ImageView imgProduct;

    private int totalSeconds = 900;
    private Timeline countdownTimeline;
    private int currentAuctionId = -1; // Đổi thành biến thay đổi được

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
    }

    // HÀM MỚI: Được gọi từ AuctionListScreenController để truyền dữ liệu thật vào
    public void initData(int auctionId, String imageUrl) {
        this.currentAuctionId = auctionId;
        
        // Load ảnh thật
        loadProductImage(imageUrl);

        // Gửi lệnh JOIN_AUCTION để Server biết User này đang theo dõi phòng này
        JsonObject joinReq = new JsonObject();
        joinReq.addProperty("type", ActionType.JOIN_AUCTION);
        joinReq.addProperty("auctionId", auctionId);
        ClientSocket.getInstance().sendJsonRequest(joinReq, null, null);

        // Kéo lịch sử thật của phiên này về (nếu muốn làm mịn hơn thì gọi GET_BID_HISTORY)
        // Hiện tại tạm để mock để giữ UI không rỗng
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
        if (currentAuctionId == -1) {
            showAlert("Lỗi", "Chưa xác định được phiên đấu giá!");
            return;
        }

        String input = txtBidAmount.getText().trim();
        if (input.isEmpty()) return;

        try {
            long bidAmount = Long.parseLong(input.replaceAll("\\D", ""));
            // bidderId truyền 0 vì Server sẽ tự xác định qua UserSession trong ClientHandler
            AuctionDTOs.BidRequest request = new AuctionDTOs.BidRequest(currentAuctionId, bidAmount, 0);

            btnPlaceBid.setDisable(true);

            JsonObject jsonRequest = new Gson().toJsonTree(request).getAsJsonObject();
            jsonRequest.addProperty("type", ActionType.PLACE_BID);

            ClientSocket.getInstance().sendJsonRequest(jsonRequest, "BID_RESPONSE", response -> {
                Platform.runLater(() -> {
                    btnPlaceBid.setDisable(false);
                    boolean success = response.has("success") && response.get("success").getAsBoolean();
                    if (!success) {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Đã có người trả giá cao hơn.";
                        showAlert("Giá không hợp lệ", msg);
                    } else {
                        checkAndApplySnipingRule();
                    }
                });
            });

            txtBidAmount.clear();

        } catch (NumberFormatException e) {
            showAlert("Lỗi nhập liệu", "Vui lòng chỉ nhập số hợp lệ!");
        }
    }

    @FXML
    private void handleEnableAutoBid() {
        if (currentAuctionId == -1) {
            showAlert("Lỗi", "Chưa xác định được phiên đấu giá!");
            return;
        }

        if (txtMaxAutoBid == null || txtMaxAutoBid.getText().trim().isEmpty()) {
            showAlert("Thiếu thông tin", "Vui lòng nhập số tiền tối đa bạn có thể trả cho sản phẩm này!");
            return;
        }

        try {
            long maxPrice = Long.parseLong(txtMaxAutoBid.getText().trim());

            JsonObject jsonRequest = new JsonObject();
            jsonRequest.addProperty("type", ActionType.REGISTER_AUTO_BID);
            jsonRequest.addProperty("auctionId", currentAuctionId);
            jsonRequest.addProperty("maxBid", maxPrice); 

            if (btnEnableAutoBid != null) btnEnableAutoBid.setDisable(true);

            ClientSocket.getInstance().sendJsonRequest(jsonRequest, "AUTO_BID_RESPONSE", response -> {
                Platform.runLater(() -> {
                    if (btnEnableAutoBid != null) btnEnableAutoBid.setDisable(false);
                    boolean success = response.has("success") && response.get("success").getAsBoolean();
                    if (success) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Thành công");
                        alert.setHeaderText(null);
                        alert.setContentText("Đã kích hoạt hệ thống Tự động đấu giá (Auto Bidding) thành công!");
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

    private void checkAndApplySnipingRule() {
        if (totalSeconds > 0 && totalSeconds <= 30) {
            totalSeconds += 60;
            lblCountdown.setStyle("-fx-text-fill: #1A0F0A; -fx-font-weight: normal;");
            updateCountdownLabel();
            org.slf4j.LoggerFactory.getLogger(getClass()).info("[Anti-Sniping] Phát hiện bid trong 30s cuối! Tự động gia hạn thêm 1 phút.");
        }
    }

    public void updateRealtimeBid(long newPrice, String bidderName) {
        lblCurrentPrice.setText(String.format("%,.0f đ", (double) newPrice));
        lblLeader.setText("Người dẫn đầu: " + bidderName);

        String historyEntry = String.format("%s: %,.0f đ", bidderName, (double) newPrice);
        lvBidHistory.getItems().add(0, historyEntry);

        checkAndApplySnipingRule();
    }

    public void loadProductImage(String urlString) {
        if (imgProduct == null) return;

        if (urlString == null || urlString.trim().isEmpty()) {
            imgProduct.setImage(null);
            return;
        }

        try {
            Image image = new Image(urlString, true);
            image.exceptionProperty().addListener((obs, oldExc, newExc) -> {
                if (newExc != null) {
                    org.slf4j.LoggerFactory.getLogger(getClass()).error("[UI Error] Không thể nạp ảnh sản phẩm từ URL: {}", newExc.getMessage());
                    Platform.runLater(() -> imgProduct.setImage(null));
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