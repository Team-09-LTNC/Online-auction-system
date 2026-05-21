package com.auction.client.controller.bidder;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.dto.AuctionDTOs;
import com.auction.common.enums.ActionType;
import com.google.gson.Gson;
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

    // --- LIÊN KẾT THÔNG TIN SẢN PHẨM ---
    @FXML private Label lblProductName;
    @FXML private Label lblDescription;
    @FXML private Label lblCategory;
    @FXML private Label lblStartingPrice; // Giá khởi điểm
    @FXML private Label lblBidIncrement;  // Bước giá
    @FXML private Label lblBuyNowPrice;   // Giá mua đứt

    // --- LIÊN KẾT TRẠNG THÁI PHÒNG ĐẤU GIÁ ---
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblCountdown;
    @FXML private Label lblLeader;
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
    private String currentStatus = "OPEN";
    private String productImageUrl = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Đăng ký controller với bộ xử lý Push Notification realtime
        com.auction.client.networkclient.PushHandler.currentRoomController = this;

        // Ràng buộc nhập số cho các ô văn bản đầu vào để đảm bảo Thread-safety và tránh lỗi định dạng
        if (txtBidAmount != null) {
            txtBidAmount.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.isEmpty()) txtBidAmount.setText(newVal.replaceAll("\\D", ""));
            });
        }
        if (txtMaxAutoBid != null) {
            txtMaxAutoBid.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.isEmpty()) txtMaxAutoBid.setText(newVal.replaceAll("\\D", ""));
            });
        }
    }

    /**
     * Khởi tạo và đồng bộ dữ liệu phòng từ Database Server
     */
    public void initData(int auctionId, String imageUrl) {
        this.currentAuctionId = auctionId;
        this.productImageUrl = imageUrl;
        loadProductImage(imageUrl);

        // 1. Gửi yêu cầu gia nhập phòng (Observer Pattern)
        JsonObject joinReq = new JsonObject();
        joinReq.addProperty("type", ActionType.JOIN_AUCTION);
        joinReq.addProperty("auctionId", auctionId);
        ClientSocket.getInstance().sendJsonRequest(joinReq, null, null);

        // 2. Tải thông tin phòng chi tiết
        refreshAuctionState();
    }

    /**
     * Gọi lên Server để cập nhật trạng thái mới nhất từ Database
     */
    private void refreshAuctionState() {
        if (currentAuctionId == -1) return;

        JsonObject getReq = new JsonObject();
        getReq.addProperty("type", ActionType.GET_AUCTION_BY_ID);
        getReq.addProperty("auctionId", currentAuctionId);

        ClientSocket.getInstance().sendJsonRequest(getReq, ActionType.GET_AUCTION_BY_ID, response -> {
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean()) {
                    JsonObject data = response.getAsJsonObject("data");
                    JsonObject itemData = data.getAsJsonObject("item");

                    // Đổ dữ liệu sản phẩm căn bản
                    if (lblProductName != null) lblProductName.setText(itemData.has("name") ? itemData.get("name").getAsString() : "Sản phẩm");
                    if (lblDescription != null) lblDescription.setText(itemData.has("description") ? itemData.get("description").getAsString() : "Không có mô tả.");
                    if (lblCategory != null) lblCategory.setText("Danh mục: " + (itemData.has("category") ? itemData.get("category").getAsString() : "Khác"));

                    // Trích xuất cấu hình giá gốc của Seller
                    long startingPrice = itemData.has("startingPrice") ? itemData.get("startingPrice").getAsLong() : 0;
                    long bidIncrement = itemData.has("bidIncrement") ? itemData.get("bidIncrement").getAsLong() : 0;

                    if (lblStartingPrice != null) lblStartingPrice.setText(String.format("%,d đ", startingPrice));
                    if (lblBidIncrement != null) lblBidIncrement.setText(String.format("%,d đ", bidIncrement));

                    // LẤY DỮ LIỆU THẬT CHO GIÁ MUA ĐỨT
                    if (lblBuyNowPrice != null) {
                        if (data.has("buyNowPrice") && !data.get("buyNowPrice").isJsonNull()) {
                            long realBuyNowPrice = data.get("buyNowPrice").getAsLong();
                            lblBuyNowPrice.setText(String.format("%,d đ", realBuyNowPrice));
                        } else {
                            lblBuyNowPrice.setText("Không hỗ trợ");
                        }
                    }

                    // ---> SỬA LỖI ĐỒNG BỘ MỨC GIÁ HIỆN TẠI TỪ SERVER <---
                    // Đọc chính xác thuộc tính định danh tuần tự của đối tượng gốc
                    long displayPrice = startingPrice;
                    if (data.has("currentHighestBid") && !data.get("currentHighestBid").isJsonNull()) {
                        displayPrice = data.get("currentHighestBid").getAsLong();
                    } else if (data.has("currentPrice") && !data.get("currentPrice").isJsonNull()) {
                        displayPrice = data.get("currentPrice").getAsLong();
                    }

                    String leaderText = "Chưa có ai đặt giá";
                    boolean hasWinner = data.has("currentWinner") && !data.get("currentWinner").isJsonNull();
                    if (hasWinner) {
                        JsonObject winnerObj = data.getAsJsonObject("currentWinner");
                        leaderText = winnerObj.has("fullName") ? winnerObj.get("fullName").getAsString() : winnerObj.get("username").getAsString();
                    }

                    if (lblCurrentPrice != null) lblCurrentPrice.setText(String.format("%,d đ", displayPrice));
                    if (lblLeader != null) lblLeader.setText(hasWinner ? "Người dẫn đầu: " + leaderText : leaderText);

                    // Xử lý đồng bộ thời gian và phân luồng nút bấm
                    String rawServerStatus = data.has("status") && !data.get("status").isJsonNull() ? data.get("status").getAsString() : "RUNNING";

                    String rawStartTime = "";
                    if (data.has("startTime") && !data.get("startTime").isJsonNull()) rawStartTime = data.get("startTime").getAsString();
                    else if (data.has("start_time") && !data.get("start_time").isJsonNull()) rawStartTime = data.get("start_time").getAsString();

                    String rawEndTime = "";
                    if (data.has("endTime") && !data.get("endTime").isJsonNull()) rawEndTime = data.get("endTime").getAsString();
                    else if (data.has("end_time") && !data.get("end_time").isJsonNull()) rawEndTime = data.get("end_time").getAsString();

                    // ĐỒNG BỘ: Mượn não của AuctionListScreenController để tính số giây chuẩn
                    AuctionListScreenController.AuctionSecondsState state =
                            AuctionListScreenController.calculateAuctionSecondsState(rawStartTime, rawEndTime);

                    this.totalSeconds = state.countdownSeconds;
                    this.currentStatus = state.finalStatus;

                    if ("OPEN".equalsIgnoreCase(currentStatus)) {
                        isAuctionStarted = false;
                        if (btnPlaceBid != null) { btnPlaceBid.setDisable(true); btnPlaceBid.setText("CHỜ MỞ BÁN"); }
                        if (txtBidAmount != null) txtBidAmount.setEditable(false);
                        if (btnEnableAutoBid != null) btnEnableAutoBid.setDisable(true);
                        startCountdown();
                    } else if ("RUNNING".equalsIgnoreCase(currentStatus)) {
                        isAuctionStarted = true;
                        if (btnPlaceBid != null) { btnPlaceBid.setDisable(false); btnPlaceBid.setText("ĐẶT GIÁ"); }
                        if (txtBidAmount != null) txtBidAmount.setEditable(true);
                        if (btnEnableAutoBid != null) btnEnableAutoBid.setDisable(false);
                        startCountdown();
                    } else {
                        setExpiredUI();
                    }

                    // Tự động tải lịch sử đấu giá thực tế từ Database
                    loadBidHistoryFromServer();
                } else {
                    showAlert("Lỗi", "Không thể đồng bộ dữ liệu phòng đấu giá từ Server.");
                }
            });
        });
    }

    /**
     * Tải danh sách lịch sử các lượt đặt giá trước đó từ database thông qua Server
     */
    private void loadBidHistoryFromServer() {
        if (currentAuctionId == -1) return;

        JsonObject historyReq = new JsonObject();
        historyReq.addProperty("type", ActionType.GET_BID_HISTORY);
        historyReq.addProperty("auctionId", currentAuctionId);

        ClientSocket.getInstance().sendJsonRequest(historyReq, ActionType.GET_BID_HISTORY, response -> {
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean() && response.has("data")) {
                    JsonArray historyArray = response.getAsJsonArray("data");
                    if (lvBidHistory != null) {
                        lvBidHistory.getItems().clear();
                        for (JsonElement el : historyArray) {
                            JsonObject bidObj = el.getAsJsonObject();

                            String name = "Người dùng";
                            if (bidObj.has("bidderName")) name = bidObj.get("bidderName").getAsString();
                            else if (bidObj.has("fullName")) name = bidObj.get("fullName").getAsString();
                            else if (bidObj.has("username")) name = bidObj.get("username").getAsString();

                            long amount = 0;
                            if (bidObj.has("bidAmount")) amount = bidObj.get("bidAmount").getAsLong();
                            else if (bidObj.has("amount")) amount = bidObj.get("amount").getAsLong();

                            lvBidHistory.getItems().add(name + " đã đặt: " + String.format("%,d đ", amount));
                        }
                    }
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
                if (isAuctionStarted && totalSeconds <= 30 && lblCountdown != null) {
                    lblCountdown.setStyle("-fx-text-fill: #A64452; -fx-font-weight: bold;");
                }
            } else {
                countdownTimeline.stop();

                // Nếu đang đếm ngược để mở bán, hết giờ thì gọi lại Server để tải trạng thái mới thành RUNNING
                if ("OPEN".equalsIgnoreCase(currentStatus)) {
                    refreshAuctionState();
                } else {
                    setExpiredUI();
                }
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
        updateCountdownLabel();
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
        isAuctionStarted = false;
        this.totalSeconds = 0;
        if (lblCountdown != null) { lblCountdown.setText("ĐÃ KẾT THÚC!"); lblCountdown.setStyle("-fx-text-fill: #888888; -fx-font-weight: bold;"); }
        if (btnPlaceBid != null) { btnPlaceBid.setDisable(true); btnPlaceBid.setText("HẾT HẠN"); }
        if (btnEnableAutoBid != null) btnEnableAutoBid.setDisable(true);
        if (txtBidAmount != null) txtBidAmount.setEditable(false);
    }

    @FXML
    private void handlePlaceBid() {
        if (!isAuctionStarted || currentAuctionId == -1) return;
        String input = txtBidAmount.getText().trim();
        if (input.isEmpty()) return;

        try {
            long bidAmount = Long.parseLong(input);

            long currentPrice = Long.parseLong(lblCurrentPrice.getText().replaceAll("\\D", ""));
            long stepPrice = Long.parseLong(lblBidIncrement.getText().replaceAll("\\D", ""));

            // Giá tối thiểu = Giá hiện tại + Bước giá
            long minValidBid = currentPrice + stepPrice;

            if (bidAmount < minValidBid) {
                showAlert("Lỗi đặt giá", "Giá tối thiểu bạn phải đặt là: " + String.format("%,d đ", minValidBid));
                return;
            }

            AuctionDTOs.BidRequest request = new AuctionDTOs.BidRequest(currentAuctionId, bidAmount, 1);
            btnPlaceBid.setDisable(true);

            JsonObject jsonRequest = new Gson().toJsonTree(request).getAsJsonObject();
            jsonRequest.addProperty("type", ActionType.PLACE_BID);

            ClientSocket.getInstance().sendJsonRequest(jsonRequest, "BID_RESPONSE", response -> {
                Platform.runLater(() -> {
                    btnPlaceBid.setDisable(false);
                    if (!(response.has("success") && response.get("success").getAsBoolean())) {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi đặt giá.";
                        showAlert("Giá thầu không hợp lệ", msg);
                    } else {
                        checkAndApplySnipingRule();
                        refreshAuctionState();
                        txtBidAmount.clear();
                    }
                });
            });
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Số tiền nhập không hợp lệ.");
        }
    }

    @FXML
    private void handleEnableAutoBid() {
        if (!isAuctionStarted || currentAuctionId == -1) return;
        if (txtMaxAutoBid == null || txtMaxAutoBid.getText().trim().isEmpty()) return;

        try {
            long maxPrice = Long.parseLong(txtMaxAutoBid.getText().trim());

            // Lấy giá hiện tại và bước giá từ giao diện
            long currentPrice = Long.parseLong(lblCurrentPrice.getText().replaceAll("\\D", ""));
            long stepPrice = Long.parseLong(lblBidIncrement.getText().replaceAll("\\D", ""));

            // Tính mức giá hợp lệ tiếp theo
            long minValidBid = currentPrice + stepPrice;

            // Auto-bid chỉ có ý nghĩa khi giá tối đa người dùng chịu trả >= mức giá hợp lệ tiếp theo
            if (maxPrice < minValidBid) {
                showAlert("Lỗi Auto-bid", "Mức giá tối đa (Max Bid) phải lớn hơn hoặc bằng mức giá hợp lệ tiếp theo (" + String.format("%,d đ", minValidBid) + ").");
                return;
            }

            JsonObject jsonRequest = new JsonObject();
            jsonRequest.addProperty("type", ActionType.REGISTER_AUTO_BID);
            jsonRequest.addProperty("auctionId", currentAuctionId);
            jsonRequest.addProperty("maxBid", maxPrice);

            btnEnableAutoBid.setDisable(true);

            ClientSocket.getInstance().sendJsonRequest(jsonRequest, "AUTO_BID_RESPONSE", response -> {
                Platform.runLater(() -> {
                    btnEnableAutoBid.setDisable(false);
                    if (response.has("success") && response.get("success").getAsBoolean()) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Thành công");
                        alert.setContentText("Hệ thống Đấu giá tự động (Auto-bid) đã kích hoạt thành công!");
                        alert.showAndWait();
                    } else {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Có lỗi xảy ra khi đăng ký Auto-bid.";
                        showAlert("Lỗi Auto-bid", msg);
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
        if (lblCurrentPrice != null) lblCurrentPrice.setText(String.format("%,d đ", newPrice));
        if (lblLeader != null) lblLeader.setText("Người dẫn đầu: " + bidderName);
        if (lvBidHistory != null) lvBidHistory.getItems().add(0, bidderName + " đã đặt: " + String.format("%,d đ", newPrice));
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