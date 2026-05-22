package com.auction.client.controller.bidder;

import com.auction.client.controller.auth.UserSession;
import com.auction.client.controller.components.ProductCardController;
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

public class AuctionListScreenController implements Initializable, com.auction.client.interfaces.CategoryFilterListener {

    private static final Logger logger = LoggerFactory.getLogger(AuctionListScreenController.class);

    @FXML private FlowPane productFlowPane;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;

    private String currentCategory = "ALL";
    private boolean isLoading = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateHeaderUserInfo();

        productFlowPane.getChildren().clear();
    }

    private void updateHeaderUserInfo() {
        try {

            String name = UserSession.getUsername() != null
                    ? UserSession.getUsername()
                    : "Người dùng";

            String role = UserSession.getCurrentRole() != null
                    ? UserSession.getCurrentRole()
                    : "BIDDER";

            if (lblHeaderName != null) {
                lblHeaderName.setText("Chào, " + name);
            }

            if (lblHeaderRole != null) {
                lblHeaderRole.setText(
                        role.substring(0,1).toUpperCase()
                                + role.substring(1).toLowerCase()
                                + " ˅"
                );
            }

        } catch (Exception e) {
            logger.error("Header error", e);
        }
    }

    @Override
    public void onCategorySelected(String category) {
        currentCategory = category;
        productFlowPane.getChildren().clear();
        loadAuctionsFromServer();
    }

    private void loadAuctionsFromServer() {

        if (isLoading) return;
        isLoading = true;

        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_ALL_AUCTIONS);
        request.addProperty("category", currentCategory);

        ClientSocket.getInstance().sendJsonRequest(
                request,
                "AUCTION_LIST_RESPONSE",
                response -> {

                    try {
                        JsonArray auctions = response.getAsJsonArray("auctions");
                        String serverNow = response.has("serverNow")
                                ? response.get("serverNow").getAsString()
                                : null;

                        List<VBox> cards = new ArrayList<>();

                        for (JsonElement el : auctions) {

                            JsonObject obj = el.getAsJsonObject();

                            String cat = obj.has("category") ? obj.get("category").getAsString() : "";
                            if (!"ALL".equalsIgnoreCase(currentCategory)
                                    && !currentCategory.equalsIgnoreCase(cat)) continue;

                            int id = obj.get("auctionId").getAsInt();
                            String name = obj.get("itemName").getAsString();
                            long price = obj.get("currentPrice").getAsLong();
                            String img = obj.get("imageUrl").getAsString();

                            String start = obj.has("startTime") ? obj.get("startTime").getAsString() : null;
                            String end = obj.has("endTime") ? obj.get("endTime").getAsString() : null;

                            AuctionTimeUtil.AuctionState state =
                                    AuctionTimeUtil.calculateState(start, end, serverNow);

                            FXMLLoader loader = new FXMLLoader(
                                    getClass().getResource("/fxml/components/ProductCard.fxml")
                            );

                            VBox card = loader.load();
                            ProductCardController controller = loader.getController();

                            controller.setProductData(
                                    id, name, price,
                                    state.countdownSeconds,
                                    state.finalStatus,
                                    img,
                                    false
                            );

                            cards.add(card);
                        }

                        Platform.runLater(() -> {
                            productFlowPane.getChildren().setAll(cards);
                            isLoading = false;
                        });

                    } catch (Exception e) {
                        logger.error("Load error", e);
                        isLoading = false;
                    }
                }
        );
    }

    public void refreshData() {
        productFlowPane.getChildren().clear();
        loadAuctionsFromServer();
    }
}