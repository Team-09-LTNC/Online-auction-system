package com.auction.client.controller.bidder;

import com.auction.client.controller.components.ProductCardController;
import com.auction.client.networkclient.ClientSocket;
import com.auction.client.util.AuctionTimeUtil;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

    @FXML private Label lblProductName;
    @FXML private Label lblDescription;
    @FXML private Label lblCategory;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblBidIncrement;
    @FXML private Label lblBuyNowPrice;

    @FXML private Label lblCurrentPrice;
    @FXML private Label lblCountdown;
    @FXML private Label lblLeader;
    @FXML private ListView<String> lvBidHistory;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private TextField txtMaxAutoBid;
    @FXML private Button btnEnableAutoBid;
    @FXML private ImageView imgProduct;

    private long endTimeMillis = 0;
    private Timeline countdownTimeline;
    private int currentAuctionId = -1;
    private boolean isAuctionStarted = false;
    private String currentStatus = "OPEN";
    private int remainingSecondsCache = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        com.auction.client.networkclient.PushHandler.currentRoomController = this;

        txtBidAmount.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isEmpty()) {
                txtBidAmount.setText(n.replaceAll("\\D", ""));
            }
        });

        txtMaxAutoBid.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isEmpty()) {
                txtMaxAutoBid.setText(n.replaceAll("\\D", ""));
            }
        });
    }

    public void initData(int auctionId, String imageUrl) {
        this.currentAuctionId = auctionId;
        loadProductImage(imageUrl);

        JsonObject join = new JsonObject();
        join.addProperty("type", ActionType.JOIN_AUCTION);
        join.addProperty("auctionId", auctionId);

        ClientSocket.getInstance().sendJsonRequest(join, null, null);

        refreshAuctionState();
    }

    private void refreshAuctionState() {

        if (countdownTimeline != null) countdownTimeline.stop();
        if (currentAuctionId == -1) return;

        JsonObject req = new JsonObject();
        req.addProperty("type", ActionType.GET_AUCTION_BY_ID);
        req.addProperty("auctionId", currentAuctionId);

        ClientSocket.getInstance().sendJsonRequest(
                req,
                ActionType.GET_AUCTION_BY_ID,
                response -> {

                    Platform.runLater(() -> {

                        if (!response.has("success") || !response.get("success").getAsBoolean()) {
                            showAlert("Lỗi", "Không tải được dữ liệu");
                            return;
                        }

                        JsonObject data = response.getAsJsonObject("data");
                        JsonObject item = data.getAsJsonObject("item");

                        lblProductName.setText(item.get("name").getAsString());
                        lblDescription.setText(item.get("description").getAsString());
                        lblCategory.setText(item.get("category").getAsString());

                        long startPrice = item.get("startingPrice").getAsLong();
                        long bidInc = item.get("bidIncrement").getAsLong();

                        lblStartingPrice.setText(String.format("%,d đ", startPrice));
                        lblBidIncrement.setText(String.format("%,d đ", bidInc));

                        long currentPrice = data.has("currentHighestBid")
                                ? data.get("currentHighestBid").getAsLong()
                                : startPrice;

                        lblCurrentPrice.setText(String.format("%,d đ", currentPrice));

                        String leader = "Chưa có ai";
                        if (data.has("currentWinner") && !data.get("currentWinner").isJsonNull()) {
                            JsonObject w = data.getAsJsonObject("currentWinner");
                            leader = w.has("fullName") ? w.get("fullName").getAsString()
                                    : w.get("username").getAsString();
                        }
                        lblLeader.setText("Người dẫn đầu: " + leader);

                        String start = data.has("startTime") ? data.get("startTime").getAsString() : null;
                        String end = data.has("endTime") ? data.get("endTime").getAsString() : null;

                        AuctionTimeUtil.AuctionState state =
                                AuctionTimeUtil.calculateState(start, end, null);

                        endTimeMillis = AuctionTimeUtil.parseToMillis(end);
                        currentStatus = state.finalStatus.toUpperCase();
                        remainingSecondsCache = state.countdownSeconds;

                        applyUIState();

                        startCountdown();
                        loadBidHistoryFromServer();
                    });
                }
        );
    }

    private void applyUIState() {

        switch (currentStatus.toUpperCase()) {

            case "OPEN":
                isAuctionStarted = false;
                btnPlaceBid.setDisable(true);
                btnPlaceBid.setText("CHỜ MỞ");
                break;

            case "RUNNING":
                isAuctionStarted = true;
                btnPlaceBid.setDisable(false);
                btnPlaceBid.setText("ĐẶT GIÁ");
                break;

            default:
                setExpiredUI();
        }
    }

    private void startCountdown() {

        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {

            long now = System.currentTimeMillis();
            long remaining = (endTimeMillis - now) / 1000;

            if (remaining <= 0) {
                setExpiredUI();
                return;
            }

            remainingSecondsCache = (int) remaining;
            updateCountdownLabel(remainingSecondsCache);

            if (isAuctionStarted && remaining <= 30) {
                lblCountdown.setStyle("-fx-text-fill:#A64452;-fx-font-weight:bold;");
            }
        }));

        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdownLabel(int sec) {

        int h = sec / 3600;
        int m = (sec % 3600) / 60;
        int s = sec % 60;

        if (!isAuctionStarted) {
            lblCountdown.setText(String.format("Sắp mở: %02d:%02d:%02d", h, m, s));
        } else {
            lblCountdown.setText(String.format("Còn lại: %02d:%02d:%02d", h, m, s));
        }
    }

    private void setExpiredUI() {

        isAuctionStarted = false;
        currentStatus = "FINISHED";

        lblCountdown.setText("ĐÃ KẾT THÚC");
        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText("HẾT HẠN");
    }

    public void updateRealtimeBid(long price, String bidder) {

        lblCurrentPrice.setText(String.format("%,d đ", price));
        lblLeader.setText("Người dẫn đầu: " + bidder);

        lvBidHistory.getItems().add(0, bidder + " đã đặt: " + String.format("%,d đ", price));
    }

    public void loadProductImage(String url) {
        try {
            if (url != null && !url.isBlank()) {
                imgProduct.setImage(new Image(url, true));
            }
        } catch (Exception e) {
            imgProduct.setImage(null);
        }
    }

    private void showAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(t);
        a.setContentText(c);
        a.showAndWait();
    }

    private void loadBidHistoryFromServer() {

        if (currentAuctionId == -1) return;

        JsonObject req = new JsonObject();
        req.addProperty("type", ActionType.GET_BID_HISTORY);
        req.addProperty("auctionId", currentAuctionId);

        ClientSocket.getInstance().sendJsonRequest(
                req,
                ActionType.GET_BID_HISTORY,
                response -> {

                    Platform.runLater(() -> {

                        if (!response.has("success") || !response.get("success").getAsBoolean()) {
                            return;
                        }

                        JsonArray arr = response.getAsJsonArray("data");
                        lvBidHistory.getItems().clear();

                        for (JsonElement el : arr) {

                            JsonObject obj = el.getAsJsonObject();

                            String name =
                                    obj.has("bidderName") ? obj.get("bidderName").getAsString()
                                            : obj.has("fullName") ? obj.get("fullName").getAsString()
                                            : obj.has("username") ? obj.get("username").getAsString()
                                            : "Người dùng";

                            long amount = obj.has("bidAmount")
                                    ? obj.get("bidAmount").getAsLong()
                                    : obj.get("amount").getAsLong();

                            lvBidHistory.getItems().add(
                                    name + " đã đặt: " + String.format("%,d đ", amount)
                            );
                        }
                    });
                }
        );
    }
}