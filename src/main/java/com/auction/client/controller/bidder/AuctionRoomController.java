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

    private int totalSeconds = 0;
    private Timeline countdownTimeline;
    private int currentAuctionId = -1;
    private boolean isAuctionStarted = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        com.auction.client.networkclient.PushHandler.currentRoomController = this;

        txtBidAmount.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) txtBidAmount.setText(newVal.replaceAll("\\D", ""));
        });

        if (txtMaxAutoBid != null) {
            txtMaxAutoBid.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.isEmpty()) txtMaxAutoBid.setText(newVal.replaceAll("\\D", ""));
            });
        }
    }

    public void initData(int auctionId, String imageUrl) {
        this.currentAuctionId = auctionId;
        loadProductImage(imageUrl);

        JsonObject joinReq = new JsonObject();
        joinReq.addProperty("type", ActionType.JOIN_AUCTION);
        joinReq.addProperty("auctionId", auctionId);
        ClientSocket.getInstance().sendJsonRequest(joinReq, null, null);

        JsonObject getReq = new JsonObject();
        getReq.addProperty("type", ActionType.GET_AUCTION_BY_ID);
        getReq.addProperty("auctionId", auctionId);

        ClientSocket.getInstance().sendJsonRequest(getReq, ActionType.GET_AUCTION_BY_ID, response -> {
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean()) {
                    JsonObject data = response.getAsJsonObject("data");

                    long currentPrice = data.has("currentHighestBid") ? data.get("currentHighestBid").getAsLong() : 0;
                    lblCurrentPrice.setText(String.format("%,.0f đ", (double) currentPrice));

                    if (data.has("highestBidderName") && lblLeader != null) {
                        lblLeader.setText("Người dẫn đầu: " + data.get("highestBidderName").getAsString());
                    }

                    String rawStartTime = data.has("startTime") ? data.get("startTime").getAsString() : null;
                    String rawEndTime = data.has("endTime") ? data.get("endTime").getAsString() : null;
                    String serverStatus = data.has("status") ? data.get("status").getAsString() : "RUNNING";

                    AuctionListScreenController.AuctionSecondsState state =
                            AuctionListScreenController.calculateAuctionSecondsState(rawStartTime, rawEndTime, serverStatus);
                    this.totalSeconds = state.countdownSeconds;

                    if ("OPEN".equalsIgnoreCase(state.finalStatus)) {
                        isAuctionStarted = false;
                        btnPlaceBid.setDisable(true);
                        txtBidAmount.setEditable(false);
                        if (btnEnableAutoBid != null) btnEnableAutoBid.setDisable(true);
                    } else if ("RUNNING".equalsIgnoreCase(state.finalStatus)) {
                        isAuctionStarted = true;
                        btnPlaceBid.setDisable(false);
                        txtBidAmount.setEditable(true);
                        if (btnEnableAutoBid != null) btnEnableAutoBid.setDisable(false);
                    } else {
                        isAuctionStarted = true;
                        this.totalSeconds = 0;
                    }

                    startCountdown();
                } else {
                    showAlert("Lỗi", "Phiên đấu giá đã kết thúc hoặc không tồn tại!");
                }
            });
        });
    }

    private void startCountdown() {
        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (totalSeconds > 0) {
                totalSeconds--;
                updateCountdownLabel();
                if (isAuctionStarted && totalSeconds <= 30) {
                    lblCountdown.setStyle("-fx-text-fill: #A64452; -fx-font-weight: bold;");
                }
            } else {
                if (!isAuctionStarted) {
                    isAuctionStarted = true;
                    this.totalSeconds = 1200; // Mở xới cho 20 phút chạy test tiếp
                    btnPlaceBid.setDisable(false);
                    txtBidAmount.setEditable(true);
                    if (btnEnableAutoBid != null) btnEnableAutoBid.setDisable(false);
                    lblCountdown.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    lblCountdown.setText("HẾT GIỜ!");
                    lblCountdown.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    btnPlaceBid.setDisable(true);
                    if (btnEnableAutoBid != null) btnEnableAutoBid.setDisable(true);
                    txtBidAmount.setEditable(false);
                    countdownTimeline.stop();
                }
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
        updateCountdownLabel();
    }

    private void updateCountdownLabel() {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        if (!isAuctionStarted) {
            lblCountdown.setText(String.format("Sắp mở: %02d:%02d:%02d", h, m, s));
            lblCountdown.setStyle("-fx-text-fill: #FFA500; -fx-font-weight: bold;");
        } else {
            lblCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));
        }
    }

    @FXML
    private void handlePlaceBid() {
        if (!isAuctionStarted) {
            showAlert("Thông báo", "Phiên đấu giá chưa mở!");
            return;
        }
        if (currentAuctionId == -1) return;

        String input = txtBidAmount.getText().trim();
        if (input.isEmpty()) return;

        try {
            long bidAmount = Long.parseLong(input.replaceAll("\\D", ""));
            AuctionDTOs.BidRequest request = new AuctionDTOs.BidRequest(currentAuctionId, bidAmount, 1);
            btnPlaceBid.setDisable(true);

            JsonObject jsonRequest = new Gson().toJsonTree(request).getAsJsonObject();
            jsonRequest.addProperty("type", ActionType.PLACE_BID);

            ClientSocket.getInstance().sendJsonRequest(jsonRequest, "BID_RESPONSE", response -> {
                Platform.runLater(() -> {
                    btnPlaceBid.setDisable(false);
                    boolean success = response.has("success") && response.get("success").getAsBoolean();
                    if (!success) {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi đặt giá.";
                        showAlert("Giá không hợp lệ", msg);
                    } else {
                        checkAndApplySnipingRule();
                    }
                });
            });
            txtBidAmount.clear();
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng chỉ nhập số hợp lệ!");
        }
    }

    @FXML
    private void handleEnableAutoBid() {
        if (!isAuctionStarted || currentAuctionId == -1) return;
        if (txtMaxAutoBid == null || txtMaxAutoBid.getText().trim().isEmpty()) return;

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
                    if (response.has("success") && response.get("success").getAsBoolean()) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Thành công");
                        alert.setContentText("Đã kích hoạt hệ thống Tự động đấu giá!");
                        alert.showAndWait();
                    }
                });
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void checkAndApplySnipingRule() {
        if (isAuctionStarted && totalSeconds > 0 && totalSeconds <= 30) {
            totalSeconds += 60;
            updateCountdownLabel();
        }
    }

    public void updateRealtimeBid(long newPrice, String bidderName) {
        lblCurrentPrice.setText(String.format("%,.0f đ", (double) newPrice));
        if (lblLeader != null) lblLeader.setText("Người dẫn đầu: " + bidderName);
        lvBidHistory.getItems().add(0, bidderName + ": " + String.format("%,.0f đ", (double) newPrice));
        checkAndApplySnipingRule();
    }

    public void loadProductImage(String urlString) {
        if (imgProduct == null || urlString == null || urlString.trim().isEmpty()) return;
        try { imgProduct.setImage(new Image(urlString, true)); } catch (Exception e) { imgProduct.setImage(null); }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}