package com.auction.client.controller.bidder;

import com.auction.client.controller.auth.UserSession;
import com.auction.client.controller.components.ProductCardController;
import com.auction.client.interfaces.RefreshableCenterContent;
import com.auction.client.networkclient.ClientSocket;
import com.auction.client.util.AuctionTimeUtil;
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

import java.net.URL;
import java.util.ResourceBundle;

public class MainDashboardController implements Initializable, RefreshableCenterContent {

    private static final Logger logger = LoggerFactory.getLogger(MainDashboardController.class);

    @FXML private FlowPane productFlowPane;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private Label lblBannerWelcome;

    @FXML private Label lblActiveAuctions;
    @FXML private Label lblEndingSoonAuctions;
    @FXML private Label lblFollowedAuctions;
    @FXML private Label lblMyBidsCount;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Bidder đã vào Dashboard chính - Đang nạp danh sách sản phẩm.");
        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadFeaturedAuctionsFromServer();
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
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(
                request,
                "DASHBOARD_STATS_RESPONSE",
                response -> Platform.runLater(() -> {

                    if (response.has("success")
                            && response.get("success").getAsBoolean()
                            && response.has("data")) {

                        JsonObject data = response.getAsJsonObject("data");

                        int activeCount = data.has("activeCount") ? data.get("activeCount").getAsInt() : 0;
                        int joinedActiveCount = data.has("joinedActiveCount")
                                ? data.get("joinedActiveCount").getAsInt()
                                : data.has("endingSoonCount") ? data.get("endingSoonCount").getAsInt() : 0;
                        int followedCount = data.has("followedCount") ? data.get("followedCount").getAsInt() : 0;
                        int myBidsCount = data.has("myBidsCount") ? data.get("myBidsCount").getAsInt() : 0;

                        if (lblActiveAuctions != null)
                            lblActiveAuctions.setText(String.valueOf(activeCount));

                        if (lblEndingSoonAuctions != null)
                            lblEndingSoonAuctions.setText(String.valueOf(joinedActiveCount));

                        if (lblFollowedAuctions != null)
                            lblFollowedAuctions.setText(String.valueOf(followedCount));

                        if (lblMyBidsCount != null)
                            lblMyBidsCount.setText(String.valueOf(myBidsCount));

                        logger.info("Đã đồng bộ thành công số liệu thống kê lên Dashboard từ Server.");
                    }
                })
        );
    }

    private void loadFeaturedAuctionsFromServer() {

        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_ALL_AUCTIONS);
        request.addProperty("featuredRunning", true);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(
                request,
                "AUCTION_LIST_RESPONSE",
                response -> {

                    if (!(response.has("success")
                            && response.get("success").getAsBoolean()
                            && response.has("auctions"))) {
                        return;
                    }

                    JsonArray auctions = response.getAsJsonArray("auctions");
                    String serverNow = getString(response, "serverNow", null);
                    java.util.List<Integer> followedIds = new java.util.ArrayList<>();
                    if (response.has("followedIds") && response.get("followedIds").isJsonArray()) {
                        for (JsonElement id : response.getAsJsonArray("followedIds")) {
                            followedIds.add(id.getAsInt());
                        }
                    }

                    java.util.List<VBox> preparedCards = new java.util.ArrayList<>();

                    for (JsonElement element : auctions) {
                        JsonObject obj = element.getAsJsonObject();

                        int auctionId = obj.has("auctionId")
                                ? obj.get("auctionId").getAsInt()
                                : -1;

                        String name = obj.has("itemName")
                                ? obj.get("itemName").getAsString()
                                : "Sản phẩm";

                        long price = obj.has("currentPrice")
                                ? obj.get("currentPrice").getAsLong()
                                : 0;

                        String imageUrl = obj.has("imageUrl")
                                ? obj.get("imageUrl").getAsString()
                                : "";

                        String serverStatus = getString(obj, "status", "");
                        if (!"RUNNING".equalsIgnoreCase(serverStatus)) {
                            continue;
                        }

                        com.auction.client.util.ImageCacheManager.preloadPreviewImage(imageUrl);

                        String rawStartTime = obj.has("startTime") && !obj.get("startTime").isJsonNull()
                                ? obj.get("startTime").getAsString()
                                : null;

                        String rawEndTime = obj.has("endTime") && !obj.get("endTime").isJsonNull()
                                ? obj.get("endTime").getAsString()
                                : null;

                        AuctionTimeUtil.AuctionState state =
                                AuctionTimeUtil.calculateState(rawStartTime, rawEndTime, serverNow);
                        if (!"RUNNING".equalsIgnoreCase(state.finalStatus) || state.countdownSeconds <= 0) {
                            logger.warn("Bỏ qua phiên nổi bật không còn RUNNING theo thời gian DB: {}", auctionId);
                            continue;
                        }
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                            VBox card = loader.load();
                            ProductCardController controller = loader.getController();
                            controller.setProductData(
                                    auctionId,
                                    name,
                                    price,
                                    state.countdownSeconds,
                                    serverStatus,
                                    imageUrl,
                                    followedIds.contains(auctionId)
                            );

                            preparedCards.add(card);

                        } catch (Exception e) {
                            logger.error("Load ProductCard lỗi", e);
                        }
                    }

                    Platform.runLater(() -> {
                        if (productFlowPane != null) {
                            productFlowPane.getChildren().clear();
                            productFlowPane.getChildren().addAll(preparedCards);
                        }
                    });
                }
        );
    }

    @Override
    public void refreshContent() {
        loadFeaturedAuctionsFromServer();
        updateStatistics();
    }

    private String getString(JsonObject obj, String key, String fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString()
                : fallback;
    }

}
