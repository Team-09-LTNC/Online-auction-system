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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class FollowedAuctionsController implements Initializable, RefreshableCenterContent {

    private static final Logger logger = LoggerFactory.getLogger(FollowedAuctionsController.class);

    @FXML private FlowPane productFlowPane;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateHeaderUserInfo();
        productFlowPane.getChildren().clear();
        loadFollowedAuctionsFromServer();
    }

    private void updateHeaderUserInfo() {
        String name = UserSession.getUsername() != null ? UserSession.getUsername() : "Người dùng";
        String role = UserSession.getCurrentRole() != null ? UserSession.getCurrentRole() : "BIDDER";

        lblHeaderName.setText("Chào, " + name);
        lblHeaderRole.setText(role.substring(0,1).toUpperCase() + role.substring(1).toLowerCase());
    }

    private void loadFollowedAuctionsFromServer() {

        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_FOLLOWED_AUCTIONS);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(
                request,
                "FOLLOWED_AUCTIONS_RESPONSE",
                response -> {

                    try {
                        if (!response.has("auctions")) return;

                        JsonArray auctions = response.getAsJsonArray("auctions");
                        String serverNow = response.has("serverNow")
                                ? response.get("serverNow").getAsString()
                                : null;

                        List<VBox> cards = new ArrayList<>();

                        for (JsonElement el : auctions) {

                            JsonObject obj = el.getAsJsonObject();

                            int id = obj.get("auctionId").getAsInt();
                            String name = obj.get("itemName").getAsString();
                            long price = obj.get("currentPrice").getAsLong();
                            String img = obj.get("imageUrl").getAsString();
                            com.auction.client.util.ImageCacheManager.preloadPreviewImage(img);

                            String start = obj.has("startTime") ? obj.get("startTime").getAsString() : null;
                            String end = obj.has("endTime") ? obj.get("endTime").getAsString() : null;

                            AuctionTimeUtil.AuctionState state =
                                    AuctionTimeUtil.calculateState(start, end, serverNow);
                            String storedStatus = obj.has("status") && !obj.get("status").isJsonNull()
                                    ? obj.get("status").getAsString()
                                    : null;

                            FXMLLoader loader = new FXMLLoader(
                                    getClass().getResource("/fxml/components/ProductCard.fxml")
                            );

                            VBox card = loader.load();
                            ProductCardController controller = loader.getController();

                            controller.setProductData(
                                    id, name, price,
                                    state.countdownSeconds,
                                    resolveDisplayStatus(storedStatus, state.finalStatus),
                                    img,
                                    true
                            );
                            JsonObject roomSnapshot = obj.deepCopy();
                            roomSnapshot.addProperty("displayStatus", resolveDisplayStatus(storedStatus, state.finalStatus));
                            roomSnapshot.addProperty("countdownSeconds", state.countdownSeconds);
                            if (serverNow != null) {
                                roomSnapshot.addProperty("serverNow", serverNow);
                            }
                            controller.setAuctionSnapshot(roomSnapshot);

                            cards.add(card);
                        }

                        Platform.runLater(() -> {
                            productFlowPane.getChildren().setAll(cards);
                        });

                    } catch (Exception e) {
                        logger.error("Followed load error", e);
                    }
                }
        );
    }

    @Override
    public void refreshContent() {
        loadFollowedAuctionsFromServer();
    }

    private String resolveDisplayStatus(String storedStatus, String timeStatus) {
        if (storedStatus == null || storedStatus.isBlank()) {
            return timeStatus;
        }

        String normalized = storedStatus.trim().toUpperCase(java.util.Locale.ROOT);
        switch (normalized) {
            case "PAID":
            case "CANCELED":
            case "FINISHED":
                return normalized;
            case "OPEN":
            case "RUNNING":
                return timeStatus;
            default:
                return normalized;
        }
    }
}
