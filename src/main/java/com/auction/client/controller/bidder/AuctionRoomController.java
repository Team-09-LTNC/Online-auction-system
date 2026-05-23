package com.auction.client.controller.bidder;

import com.auction.client.networkclient.ClientSocket;
import com.auction.client.util.AuctionTimeUtil;
import com.auction.common.dto.AuctionDTOs;
import com.auction.common.enums.ActionType;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @FXML private AreaChart<String, Number> priceChart;
    @FXML private CategoryAxis timeAxis;
    @FXML private NumberAxis priceAxis;

    private XYChart.Series<String, Number> priceSeries;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm-dd");
    private final DateTimeFormatter historyTimeFormatter = DateTimeFormatter.ofPattern("dd HH:mm:ss");

    private int totalSeconds = 0;
    private Timeline countdownTimeline;

    private int currentAuctionId = -1;
    private boolean isAuctionStarted = false;
    private String currentStatus = "OPEN";

    private Long currentBuyNowPrice;
    private long currentPriceValue = 0;

    private boolean snipingExtended = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        com.auction.client.networkclient.PushHandler.currentRoomController = this;

        setupNumberOnlyInput(txtBidAmount);
        setupNumberOnlyInput(txtMaxAutoBid);

        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá đấu");

        if (priceChart != null) {
            priceChart.getData().add(priceSeries);
        }
    }

    private void setupNumberOnlyInput(TextField textField) {
        if (textField == null) return;

        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                textField.setText(newVal.replaceAll("\\D", ""));
            }
        });
    }

    public void initData(int auctionId, String imageUrl) {
        this.currentAuctionId = auctionId;
        loadProductImage(imageUrl);

        JsonObject joinReq = new JsonObject();
        joinReq.addProperty("type", ActionType.JOIN_AUCTION);
        joinReq.addProperty("auctionId", auctionId);
        joinReq.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(joinReq, null, null);
        refreshAuctionState();
    }

    private void refreshAuctionState() {
        if (currentAuctionId == -1) return;

        JsonObject getReq = new JsonObject();
        getReq.addProperty("type", ActionType.GET_AUCTION_BY_ID);
        getReq.addProperty("auctionId", currentAuctionId);
        getReq.addProperty("requestId", java.util.UUID.randomUUID().toString());

        System.out.println("DEBUG ROOM - Gửi GET_AUCTION_BY_ID: " + getReq);

        ClientSocket.getInstance().sendJsonRequest(
                getReq,
                ActionType.GET_AUCTION_BY_ID,
                response -> Platform.runLater(() -> {
                    System.out.println("DEBUG ROOM - Nhận GET_AUCTION_BY_ID: " + response);
                    handleAuctionDetailResponse(response);
                })
        );
    }

    private void handleAuctionDetailResponse(JsonObject response) {
        if (!(response.has("success") && response.get("success").getAsBoolean())) {
            return;
        }

        JsonObject data = response.getAsJsonObject("data");
        JsonObject itemData = data.getAsJsonObject("item");

        updateProductInfo(data, itemData);
        updateLeaderInfo(data);

        String rawStartTime = data.has("startTime") && !data.get("startTime").isJsonNull() ? data.get("startTime").getAsString() : "";

        String rawEndTime = data.has("endTime") && !data.get("endTime").isJsonNull() ? data.get("endTime").getAsString() : "";

        String serverNow = response.has("serverNow") && !response.get("serverNow").isJsonNull()
                ? response.get("serverNow").getAsString()
                : null;

        AuctionTimeUtil.AuctionState state =
                AuctionTimeUtil.calculateState(rawStartTime, rawEndTime, serverNow);

        this.totalSeconds = Math.max(state.countdownSeconds, 0);
        this.currentStatus = state.finalStatus != null ? state.finalStatus.toUpperCase() : "OPEN";

        this.snipingExtended = false;

        if ("OPEN".equalsIgnoreCase(currentStatus) || "RUNNING".equalsIgnoreCase(currentStatus)) {

            isAuctionStarted = "RUNNING".equalsIgnoreCase(currentStatus);
            updateBidControls(data);
            startCountdown();

        } else {setExpiredUI();}

        loadBidHistoryFromServer();
    }

    private void updateProductInfo(JsonObject data, JsonObject itemData) {
        if (lblProductName != null) {
            lblProductName.setText(itemData.has("name") ? itemData.get("name").getAsString() : "Sản phẩm");
        }

        if (lblDescription != null) {
            lblDescription.setText(itemData.has("description") ? itemData.get("description").getAsString() : "Không có mô tả.");
        }

        if (lblCategory != null) {
            lblCategory.setText("Danh mục: " + (itemData.has("category") ? itemData.get("category").getAsString() : "Khác"));
        }

        long startingPrice = itemData.has("startingPrice") ? itemData.get("startingPrice").getAsLong() : 0;

        long bidIncrement = itemData.has("bidIncrement") ? itemData.get("bidIncrement").getAsLong() : 0;

        if (lblStartingPrice != null) {
            lblStartingPrice.setText(String.format("%,d đ", startingPrice));
        }

        if (lblBidIncrement != null) {
            lblBidIncrement.setText(String.format("%,d đ", bidIncrement));
        }

        currentBuyNowPrice = data.has("buyNowPrice") && !data.get("buyNowPrice").isJsonNull() ? data.get("buyNowPrice").getAsLong() : null;

        if (lblBuyNowPrice != null) {
            lblBuyNowPrice.setText(currentBuyNowPrice != null && currentBuyNowPrice > 0 ? String.format("%,d đ", currentBuyNowPrice) : "Không hỗ trợ");
        }

        long displayPrice = data.has("currentHighestBid") && !data.get("currentHighestBid").isJsonNull() ? data.get("currentHighestBid").getAsLong() : startingPrice;

        currentPriceValue = displayPrice;

        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(String.format("%,d đ", displayPrice));
        }
    }

    private void updateLeaderInfo(JsonObject data) {
        String leaderText = "Chưa có ai đặt giá";
        boolean hasWinner = false;

        JsonObject userObj =
                data.has("currentWinner") && !data.get("currentWinner").isJsonNull()
                        ? data.getAsJsonObject("currentWinner")
                        : data.has("currentHighestBidder") && !data.get("currentHighestBidder").isJsonNull()
                        ? data.getAsJsonObject("currentHighestBidder")
                        : data.has("highestBidder") && !data.get("highestBidder").isJsonNull()
                        ? data.getAsJsonObject("highestBidder")
                        : null;

        if (userObj != null) {
            leaderText = userObj.has("fullName") ? userObj.get("fullName").getAsString() : userObj.get("username").getAsString();

            hasWinner = true;
        }

        if (lblLeader != null) {
            lblLeader.setText(hasWinner ? "Người dẫn đầu: " + leaderText : leaderText);
        }
    }

    private void updateBidControls(JsonObject data) {
        if (btnPlaceBid != null) {
            btnPlaceBid.setDisable(!isAuctionStarted);
            btnPlaceBid.setText(isAuctionStarted ? "ĐẶT GIÁ" : "CHỜ MỞ BÁN");
        }

        if (txtBidAmount != null) {
            txtBidAmount.setEditable(isAuctionStarted);
        }

        if (btnEnableAutoBid != null) {
            btnEnableAutoBid.setDisable(!isAuctionStarted);
        }

        if (data.has("userMaxAutoBid") && !data.get("userMaxAutoBid").isJsonNull()) {
            long savedMaxBid = data.get("userMaxAutoBid").getAsLong();

            if (txtMaxAutoBid != null) {
                txtMaxAutoBid.setText(String.valueOf(savedMaxBid));
            }
            if (btnEnableAutoBid != null) {
                btnEnableAutoBid.setText("CẬP NHẬT AUTO-BID");
            }
        } else {
            if (txtMaxAutoBid != null) {
                txtMaxAutoBid.clear();
            }
            if (btnEnableAutoBid != null) {
                btnEnableAutoBid.setText("ĐĂNG KÝ AUTO-BID");
            }
        }
    }

    private void loadBidHistoryFromServer() {
        if (currentAuctionId == -1) return;

        JsonObject historyReq = new JsonObject();
        historyReq.addProperty("type", ActionType.GET_BID_HISTORY);
        historyReq.addProperty("auctionId", currentAuctionId);
        historyReq.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(historyReq, ActionType.GET_BID_HISTORY, response -> Platform.runLater(() -> handleBidHistoryResponse(response)));
    }

    private void handleBidHistoryResponse(JsonObject response) {
        if (!(response.has("success")
                && response.get("success").getAsBoolean()
                && response.has("data"))) {
            return;
        }
        JsonArray historyArray = response.getAsJsonArray("data");

        if (lvBidHistory != null) {
            lvBidHistory.getItems().clear();
        }

        if (priceChart != null && priceSeries != null) {
            priceChart.getData().remove(priceSeries);
        }

        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá đấu");

        List<XYChart.Data<String, Number>> chartPoints = new ArrayList<>();
        String latestLeader = null;

        for (JsonElement el : historyArray) {
            JsonObject bidObj = el.getAsJsonObject();
            String name =
                    bidObj.has("fullName")
                            ? bidObj.get("fullName").getAsString()
                            : bidObj.has("bidderName")
                            ? bidObj.get("bidderName").getAsString()
                            : bidObj.has("username")
                            ? bidObj.get("username").getAsString()
                            : "Người dùng ẩn danh";

            long amount =
                    bidObj.has("bidAmount")
                            ? bidObj.get("bidAmount").getAsLong()
                            : bidObj.has("amount")
                            ? bidObj.get("amount").getAsLong()
                            : 0;

            latestLeader = name;

            String rawTime =
                    bidObj.has("bidTime")
                            ? bidObj.get("bidTime").getAsString()
                            : LocalDateTime.now().toString();

            String chartTimeStr;
            String historyTimeStr;

            try {
                LocalDateTime dt = LocalDateTime.parse(rawTime);
                chartTimeStr = dt.format(timeFormatter);
                historyTimeStr = dt.format(historyTimeFormatter);
            } catch (Exception e) {
                chartTimeStr = LocalDateTime.now().format(timeFormatter);
                historyTimeStr = LocalDateTime.now().format(historyTimeFormatter);
            }

            if (lvBidHistory != null) {
                lvBidHistory.getItems().add(0, "(" + historyTimeStr + ") " + name + " đã đặt: " + String.format("%,d đ", amount));
            }

            XYChart.Data<String, Number> dataPoint =
                    new XYChart.Data<>(chartTimeStr, amount);

            setupHoverEffect(dataPoint);
            chartPoints.add(dataPoint);
        }

        priceSeries.getData().addAll(chartPoints);

        if (priceChart != null) {
            priceChart.getData().add(priceSeries);
        }

        if (latestLeader != null && !latestLeader.trim().isEmpty() && !"null".equals(latestLeader)) {

            if (lblLeader != null) {
                lblLeader.setText("Người dẫn đầu: " + latestLeader);
            }

        } else {
            if (lblLeader != null) {
                lblLeader.setText("Chưa có ai đặt giá");
            }
        }
    }

    private void startCountdown() {
        stopCountdown();

        countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    if (totalSeconds > 0) {
                        totalSeconds--;

                        updateCountdownLabel();

                        if (isAuctionStarted
                                && totalSeconds <= 30
                                && lblCountdown != null) {

                            lblCountdown.setStyle(
                                    "-fx-text-fill: #A64452; -fx-font-weight: bold;"
                            );
                        }

                    } else {
                        stopCountdown();

                        if ("OPEN".equalsIgnoreCase(currentStatus)) {
                            refreshAuctionState();
                        } else {
                            setExpiredUI();
                        }
                    }
                })
        );

        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();

        updateCountdownLabel();
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private void updateCountdownLabel() {
        if (lblCountdown == null) return;

        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;

        if (!isAuctionStarted) {
            lblCountdown.setText(String.format("Sắp mở: %02d:%02d:%02d", h, m, s));
            lblCountdown.setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold;");

        } else {
            lblCountdown.setText(String.format("Còn lại: %02d:%02d:%02d", h, m, s));
            lblCountdown.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
        }
    }

    private void setExpiredUI() {
        stopCountdown();

        isAuctionStarted = false;
        totalSeconds = 0;

        if (lblCountdown != null) {
            lblCountdown.setText("ĐÃ KẾT THÚC!");
            lblCountdown.setStyle(
                    "-fx-text-fill: #888888; -fx-font-weight: bold;"
            );
        }

        if (btnPlaceBid != null) {
            btnPlaceBid.setDisable(true);
            btnPlaceBid.setText("HẾT HẠN");
        }

        if (btnEnableAutoBid != null) {
            btnEnableAutoBid.setDisable(true);
        }

        if (txtBidAmount != null) {
            txtBidAmount.setEditable(false);
        }
    }

    @FXML
    private void handlePlaceBid() {
        if (!isAuctionStarted || currentAuctionId == -1) return;

        String input = txtBidAmount.getText().trim();
        if (input.isEmpty()) return;

        try {
            long bidAmount = Long.parseLong(input);
            long currentPrice = currentPriceValue;

            long stepPrice =
                    Long.parseLong(lblBidIncrement.getText().replaceAll("\\D", ""));

            long minValidBid = currentPrice + stepPrice;

            if (bidAmount < minValidBid) {
                showAlert("Lỗi đặt giá", "Giá tối thiểu: " + String.format("%,d đ", minValidBid));
                return;
            }

            if (currentBuyNowPrice != null && currentBuyNowPrice > 0 && bidAmount >= currentBuyNowPrice) {

                if (xacNhanMuaDut()) {
                    guiXacNhanMuaDut();
                }
                return;
            }
            AuctionDTOs.BidRequest request = new AuctionDTOs.BidRequest(currentAuctionId, bidAmount, 1);

            if (btnPlaceBid != null) {
                btnPlaceBid.setDisable(true);
            }

            JsonObject jsonRequest = new Gson().toJsonTree(request).getAsJsonObject();

            jsonRequest.addProperty("type", ActionType.PLACE_BID);

            ClientSocket.getInstance().sendJsonRequest(jsonRequest, "BID_RESPONSE", response -> Platform.runLater(() -> {
                        if (btnPlaceBid != null) {
                            btnPlaceBid.setDisable(false);
                        }

                        if (!(response.has("success")
                                && response.get("success").getAsBoolean())) {

                            showAlert(
                                    "Lỗi",
                                    response.has("message")
                                            ? response.get("message").getAsString()
                                            : "Lỗi đặt giá."
                            );

                        } else {
                            checkAndApplySnipingRule();
                            refreshAuctionState();

                            if (txtBidAmount != null) {
                                txtBidAmount.clear();
                            }
                        }
                    })
            );

        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Số tiền không hợp lệ.");
        }
    }

    private boolean xacNhanMuaDut() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Xác nhận mua đứt");
        dialog.setHeaderText(null);

        ButtonType cancelButton = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);

        ButtonType confirmButton = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().setAll(cancelButton, confirmButton);

        Label message = new Label(
                "Bạn đã đặt giá vượt quá giá mua đứt của sản phẩm.\n\n"
                        + "Xác nhận nếu bạn muốn sở hữu sản phẩm này ngay lập tức.\n\n"
                        + "Hủy nếu bạn muốn đặt một mức giá thấp hơn."
        );

        message.setWrapText(true);
        message.setStyle(
                "-fx-font-size: 14px; -fx-text-fill: #342724; -fx-line-spacing: 2px;"
        );

        dialog.getDialogPane().setContent(message);
        dialog.getDialogPane().setPrefWidth(450);
        dialog.getDialogPane().setStyle(
                "-fx-background-color: #FFFDFC; -fx-padding: 14px;"
        );

        Button cancel = (Button) dialog.getDialogPane().lookupButton(cancelButton);

        cancel.setStyle(
                "-fx-background-color: #F0ECE8; -fx-text-fill: #3E2723; "
                        + "-fx-font-weight: bold; -fx-background-radius: 7; -fx-padding: 9 20;"
        );

        Button confirm = (Button) dialog.getDialogPane().lookupButton(confirmButton);

        confirm.setStyle(
                "-fx-background-color: #B32638; -fx-text-fill: white; "
                        + "-fx-font-weight: bold; -fx-background-radius: 7; -fx-padding: 9 20;"
        );

        Optional<ButtonType> selected = dialog.showAndWait();

        return selected.isPresent() && selected.get() == confirmButton;
    }

    private void guiXacNhanMuaDut() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.CONFIRM_BUY_NOW);
        request.addProperty("auctionId", currentAuctionId);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());

        if (btnPlaceBid != null) {
            btnPlaceBid.setDisable(true);
        }

        ClientSocket.getInstance().sendJsonRequest(request, "BUY_NOW_RESPONSE", response -> Platform.runLater(() -> {
                    if (btnPlaceBid != null) {
                        btnPlaceBid.setDisable(false);
                    }

                    if (!(response.has("success")
                            && response.get("success").getAsBoolean())) {

                        showAlert("Không thể mua đứt",
                                response.has("message")
                                        ? response.get("message").getAsString()
                                        : "Mua đứt thất bại."
                        );

                        return;
                    }

                    showBuyNowWinnerDialog();

                    if (txtBidAmount != null) {
                        txtBidAmount.clear();
                    }

                    refreshAuctionState();
                })
        );
    }

    private void showBuyNowWinnerDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Chiến thắng phiên đấu giá");

        alert.setHeaderText("Chúc mừng! Bạn đã là người chiến thắng ở phiên đấu giá này."
        );

        alert.setContentText(
                "Vui lòng thanh toán để chính thức sở hữu sản phẩm.\n\n"
                        + "Nếu hủy thanh toán, bạn sẽ chịu phạt 10% tiền đặt giá.");

        alert.getDialogPane().setStyle("-fx-background-color: #FFFDFC; -fx-font-size: 13px;");

        alert.showAndWait();
    }

    @FXML
    private void handleEnableAutoBid() {
        if (!isAuctionStarted || currentAuctionId == -1 || txtMaxAutoBid == null || txtMaxAutoBid.getText().trim().isEmpty()) {
            return;
        }

        try {
            long maxPrice = Long.parseLong(txtMaxAutoBid.getText().trim());

            long currentPrice = currentPriceValue;

            long minValidBid = currentPrice + Long.parseLong(lblBidIncrement.getText().replaceAll("\\D", ""));

            if (maxPrice < minValidBid) {
                showAlert("Lỗi Auto-bid", "Mức giá tối đa phải >= " + String.format("%,d đ", minValidBid));
                return;
            }

            JsonObject jsonRequest = new JsonObject();

            jsonRequest.addProperty("type", ActionType.REGISTER_AUTO_BID);
            jsonRequest.addProperty("auctionId", currentAuctionId);
            jsonRequest.addProperty("maxBid", maxPrice);

            if (btnEnableAutoBid != null) {
                btnEnableAutoBid.setDisable(true);
            }

            ClientSocket.getInstance().sendJsonRequest(jsonRequest, "AUTO_BID_RESPONSE", response -> Platform.runLater(() -> {
                        if (btnEnableAutoBid != null) {
                            btnEnableAutoBid.setDisable(false);
                        }

                        if (response.has("success")
                                && response.get("success").getAsBoolean()) {

                            Alert alert =
                                    new Alert(Alert.AlertType.INFORMATION);

                            alert.setTitle("Thành công");
                            alert.setContentText(
                                    "Kích hoạt Auto-bid thành công!"
                            );
                            alert.showAndWait();

                        } else {
                            showAlert(
                                    "Lỗi Auto-bid",
                                    response.has("message")
                                            ? response.get("message").getAsString()
                                            : "Lỗi đăng ký."
                            );
                        }
                    })
            );

        } catch (Exception e) {
            showAlert("Lỗi Auto-bid", "Số tiền auto-bid không hợp lệ.");
        }
    }

    private void checkAndApplySnipingRule() {
        if (isAuctionStarted
                && totalSeconds > 0
                && totalSeconds <= 30
                && !snipingExtended) {

            totalSeconds += 60;
            snipingExtended = true;

            updateCountdownLabel();
        }
    }

    public void updateRealtimeBid(long newPrice, String bidderName) {
        currentPriceValue = newPrice;

        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(String.format("%,d đ", newPrice));
        }

        if (lblLeader != null) {
            lblLeader.setText("Người dẫn đầu: " + bidderName);
        }

        LocalDateTime now = LocalDateTime.now();

        if (lvBidHistory != null) {
            lvBidHistory.getItems().add(
                    0,
                    "(" + now.format(historyTimeFormatter) + ") "
                            + bidderName
                            + " đã đặt: "
                            + String.format("%,d đ", newPrice)
            );
        }

        checkAndApplySnipingRule();

        if (priceSeries != null) {
            XYChart.Data<String, Number> dataPoint =
                    new XYChart.Data<>(now.format(timeFormatter), newPrice);

            setupHoverEffect(dataPoint);

            priceSeries.getData().add(dataPoint);

            if (priceSeries.getData().size() > 30) {
                priceSeries.getData().remove(0);
            }
        }
    }

    public void loadProductImage(String urlString) {
        if (imgProduct == null
                || urlString == null
                || urlString.trim().isEmpty()) {
            return;
        }

        try {
            imgProduct.setImage(
                    com.auction.client.util.ImageCacheManager
                            .getPreviewImage(urlString)
            );

        } catch (Exception e) {
            imgProduct.setImage(null);
        }
    }

    public void updateRealtimeStatus(String newStatus) {
        currentStatus = newStatus;

        if ("FINISHED".equalsIgnoreCase(newStatus)
                || "PAID".equalsIgnoreCase(newStatus)
                || "CANCELLED".equalsIgnoreCase(newStatus)
                || "CANCELED".equalsIgnoreCase(newStatus)) {

            setExpiredUI();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void setupHoverEffect(XYChart.Data<String, Number> dataPoint) {
        dataPoint.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                String info = String.format("Thời gian: %s\nGiá: %,d đ", dataPoint.getXValue(), dataPoint.getYValue().longValue()
                        );
                Tooltip tooltip = new Tooltip(info);
                tooltip.setStyle(
                        "-fx-font-size: 13px;"
                                + "-fx-font-weight: bold;"
                                + "-fx-background-color: linear-gradient(#3E2723, #5D4037);"
                                + "-fx-text-fill: white;"
                                + "-fx-padding: 8px;"
                                + "-fx-background-radius: 5px;"
                                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 5);"
                );

                tooltip.setShowDelay(Duration.ZERO);
                tooltip.setHideDelay(Duration.ZERO);

                Tooltip.install(newNode, tooltip);

                ScaleTransition st = new ScaleTransition(Duration.millis(600), newNode);

                st.setFromX(0);
                st.setFromY(0);
                st.setToX(1.0);
                st.setToY(1.0);
                st.setInterpolator(Interpolator.EASE_OUT);
                st.play();

                newNode.setOnMouseEntered(e -> {newNode.setStyle("-fx-scale-x: 2.2;" + "-fx-scale-y: 2.2;" + "-fx-cursor: hand;" + "-fx-background-color: #E65100, white;"
                    );
                    newNode.toFront();
                });
                newNode.setOnMouseExited(e -> {newNode.setStyle("-fx-scale-x: 1.0;" + "-fx-scale-y: 1.0;" + "-fx-background-color: white, #A64452;");
                });
            }
        });
    }
}