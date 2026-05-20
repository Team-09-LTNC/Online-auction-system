package com.auction.client.controller.bidder;

import com.auction.client.controller.auth.UserSession;
import com.auction.client.controller.components.ProductCardController;
import com.auction.client.networkclient.ClientSocket;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

public class MainDashboardController implements Initializable, com.auction.client.interfaces.CategoryFilterListener {

    private static final Logger logger = LoggerFactory.getLogger(MainDashboardController.class);

    @FXML private FlowPane productFlowPane;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private Label lblBannerWelcome;

    @FXML private Label lblActiveAuctions;
    @FXML private Label lblEndingSoonAuctions;
    @FXML private Label lblFollowedAuctions;
    @FXML private Label lblMyBidsCount;

    private static final DateTimeFormatter MULTI_FORMATTER = DateTimeFormatter.ofPattern(
            "[yyyy-MM-dd HH:mm:ss][yyyy-MM-dd'T'HH:mm:ss][yyyy-MM-dd'T'HH:mm:ss.SSS]"
    );
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Bidder đã vào Dashboard chính - Đang nạp danh sách sản phẩm.");
        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadProductsFromServer();
        }
        updateDashboardUserInfo();
        updateStatistics();
    }

    private void updateDashboardUserInfo() {
        try {
            String currentUserName = UserSession.getUsername() != null ? UserSession.getUsername() : "Người dùng";
            String currentUserRole = UserSession.getCurrentRole() != null ? UserSession.getCurrentRole() : "BIDDER";

            if (lblHeaderName != null) lblHeaderName.setText("Chào, " + currentUserName);
            if (lblBannerWelcome != null) lblBannerWelcome.setText("Chào mừng trở lại, " + currentUserName + "! 👋");

            if (lblHeaderRole != null) {
                lblHeaderRole.setText("SELLER".equalsIgnoreCase(currentUserRole) ? "Seller" : "Bidder");
            }
        } catch (Exception e) {
            logger.error("Lỗi khi load thông tin User lên Header: {}", e.getMessage());
        }
    }

    private void updateStatistics() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_DASHBOARD_STATS);

        ClientSocket.getInstance().sendJsonRequest(request, "DASHBOARD_STATS_RESPONSE", response -> {
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean() && response.has("data")) {
                    JsonObject data = response.getAsJsonObject("data");

                    int activeCount = data.has("activeCount") ? data.get("activeCount").getAsInt() : 0;
                    int endingSoonCount = data.has("endingSoonCount") ? data.get("endingSoonCount").getAsInt() : 0;
                    int followedCount = data.has("followedCount") ? data.get("followedCount").getAsInt() : 0;
                    int myBidsCount = data.has("myBidsCount") ? data.get("myBidsCount").getAsInt() : 0;

                    if (lblActiveAuctions != null) lblActiveAuctions.setText(String.valueOf(activeCount));
                    if (lblEndingSoonAuctions != null) lblEndingSoonAuctions.setText(String.valueOf(endingSoonCount));
                    if (lblFollowedAuctions != null) lblFollowedAuctions.setText(String.valueOf(followedCount));
                    if (lblMyBidsCount != null) lblMyBidsCount.setText(String.valueOf(myBidsCount));

                    logger.info("Đã đồng bộ thành công số liệu thống kê lên Dashboard từ Server.");
                }
            });
        });
    }

    private void loadProductsFromServer() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_ALL_AUCTIONS);

        ClientSocket.getInstance().sendJsonRequest(request, "AUCTION_LIST_RESPONSE", response -> {
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean() && response.has("auctions")) {
                    JsonArray auctions = response.getAsJsonArray("auctions");

                    if (productFlowPane != null) {
                        productFlowPane.getChildren().clear();
                    }

                    int count = 0;
                    for (JsonElement element : auctions) {
                        if (count >= 6) break;

                        JsonObject obj = element.getAsJsonObject();
                        int auctionId = obj.has("auctionId") ? obj.get("auctionId").getAsInt() : -1;
                        String name = obj.has("itemName") ? obj.get("itemName").getAsString() : "Sản phẩm";
                        long price = obj.has("currentPrice") ? obj.get("currentPrice").getAsLong() : 0;
                        String status = obj.has("status") ? obj.get("status").getAsString() : "N/A";
                        String imageUrl = obj.has("imageUrl") ? obj.get("imageUrl").getAsString() : "";

                        String rawStartTime = obj.has("startTime") && !obj.get("startTime").isJsonNull() ? obj.get("startTime").getAsString() : "";
                        String rawEndTime = obj.has("endTime") && !obj.get("endTime").isJsonNull() ? obj.get("endTime").getAsString() : "";

                        // FIX CỨNG: Tính số giây thực tế từ DB qua múi giờ Ho_Chi_Minh thay vì gán bừa 300/1800 giây!
                        int calculatedSeconds = calculateRealSeconds(rawStartTime, rawEndTime, status);

                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                            VBox card = loader.load();
                            ProductCardController controller = loader.getController();

                            controller.setProductData(auctionId, name, price, calculatedSeconds, status, imageUrl);

                            productFlowPane.getChildren().add(card);
                            count++;
                        } catch (IOException e) {
                            logger.error("Không nạp được ProductCard.fxml: {}", e.getMessage());
                        }
                    }
                }
            });
        });
    }

    private int calculateRealSeconds(String rawStart, String rawEnd, String status) {
        if (rawEnd == null || rawEnd.trim().isEmpty()) {
            return "OPEN".equalsIgnoreCase(status) ? 3600 : 7200;
        }
        try {
            ZonedDateTime nowZoned = ZonedDateTime.now(VIETNAM_ZONE);
            LocalDateTime localEnd = LocalDateTime.parse(rawEnd.trim().replace(" ", "T"), MULTI_FORMATTER);
            ZonedDateTime endZoned = localEnd.atZone(ZoneId.of("UTC")).withZoneSameInstant(VIETNAM_ZONE);

            long diff = ChronoUnit.SECONDS.between(nowZoned, endZoned);

            if ("OPEN".equalsIgnoreCase(status) && rawStart != null && !rawStart.trim().isEmpty()) {
                LocalDateTime localStart = LocalDateTime.parse(rawStart.trim().replace(" ", "T"), MULTI_FORMATTER);
                ZonedDateTime startZoned = localStart.atZone(ZoneId.of("UTC")).withZoneSameInstant(VIETNAM_ZONE);
                diff = ChronoUnit.SECONDS.between(nowZoned, startZoned);
            }

            return diff > 0 ? (int) diff : 0;
        } catch (Exception e) {
            return "OPEN".equalsIgnoreCase(status) ? 600 : 1200;
        }
    }

    @Override
    public void onCategorySelected(String category) {
        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadProductsFromServer();
        }
    }
}