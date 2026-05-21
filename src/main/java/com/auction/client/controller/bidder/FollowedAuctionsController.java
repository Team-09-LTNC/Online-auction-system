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
import java.util.ResourceBundle;

public class FollowedAuctionsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(FollowedAuctionsController.class);

    @FXML private FlowPane productFlowPane;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateHeaderUserInfo();
        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadFollowedAuctionsFromServer();
        }
    }

    private void updateHeaderUserInfo() {
        try {
            String currentUserName = UserSession.getUsername() != null ? UserSession.getUsername() : "Người dùng";
            String currentUserRole = UserSession.getCurrentRole() != null ? UserSession.getCurrentRole() : "BIDDER";
            if (lblHeaderName != null) lblHeaderName.setText("Chào, " + currentUserName);
            if (lblHeaderRole != null) lblHeaderRole.setText(currentUserRole.substring(0, 1).toUpperCase() + currentUserRole.substring(1).toLowerCase() + " ˅");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadFollowedAuctionsFromServer() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_FOLLOWED_AUCTIONS);

        ClientSocket.getInstance().sendJsonRequest(request, "FOLLOWED_AUCTIONS_RESPONSE", response -> {
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean() && response.has("auctions")) {
                    JsonArray auctions = response.getAsJsonArray("auctions");

                    for (JsonElement element : auctions) {
                        JsonObject obj = element.getAsJsonObject();

                        int auctionId = obj.has("auctionId") ? obj.get("auctionId").getAsInt() : -1;
                        String name = obj.has("itemName") ? obj.get("itemName").getAsString() : "Sản phẩm";
                        long price = obj.has("currentPrice") ? obj.get("currentPrice").getAsLong() : 0;

                        String imageUrl = obj.has("imageUrl") ? obj.get("imageUrl").getAsString() : "";

                        String status = obj.has("status") && !obj.get("status").isJsonNull() ? obj.get("status").getAsString() : "RUNNING";

                        String startTimeStr = null;
                        if (obj.has("startTime") && !obj.get("startTime").isJsonNull()) startTimeStr = obj.get("startTime").getAsString();
                        else if (obj.has("start_time") && !obj.get("start_time").isJsonNull()) startTimeStr = obj.get("start_time").getAsString();

                        String endTimeStr = null;
                        if (obj.has("endTime") && !obj.get("endTime").isJsonNull()) endTimeStr = obj.get("endTime").getAsString();
                        else if (obj.has("end_time") && !obj.get("end_time").isJsonNull()) endTimeStr = obj.get("end_time").getAsString();

                        // Đồng bộ bộ não thời gian 100% với file trung tâm
                        AuctionListScreenController.AuctionSecondsState state =
                                AuctionListScreenController.calculateAuctionSecondsState(startTimeStr, endTimeStr, status);

                        // Bẻ gãy logic đếm ngược bị khùng của Card
                        // Nếu món đồ đã thật sự về 0, không nên vẽ Card ra nữa, hoặc phải ép nó thành FINISHED để Card hiện "Đã kết thúc"
                        String uiStatus = state.finalStatus;
                        if (state.countdownSeconds <= 0 && !"FINISHED".equals(uiStatus)) {
                            uiStatus = "FINISHED"; // Ép chết trạng thái để không bị lặp đếm ngầm
                        }

                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                            VBox card = loader.load();
                            ProductCardController controller = loader.getController();

                            controller.setProductData(auctionId, name, price, state.countdownSeconds, uiStatus, imageUrl, true);
                            productFlowPane.getChildren().add(card);
                        } catch (IOException e) {
                            logger.error("Lỗi nạp Card UI trong Followed: {}", e.getMessage());
                        }
                    }
                }
            });
        });
    }
}