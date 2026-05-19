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

    // Khai báo các Node quản lý thông tin tài khoản trên Header
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Đang nạp danh sách sản phẩm Bidder đang theo dõi.");

        // 1. Đồng bộ thông tin người dùng thật
        updateHeaderUserInfo();

        // 2. Tải danh sách sản phẩm qua Socket
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
            if (lblHeaderRole != null) {
                lblHeaderRole.setText(currentUserRole.substring(0, 1).toUpperCase() + currentUserRole.substring(1).toLowerCase() + " ˅");
            }
        } catch (Exception e) {
            logger.error("Lỗi cập nhật Header tại FollowedAuctions: {}", e.getMessage());
        }
    }

    private void loadFollowedAuctionsFromServer() {
        JsonObject request = new JsonObject();
        // Gửi ActionType yêu cầu lấy danh sách Follow
        request.addProperty("type", ActionType.GET_FOLLOWED_AUCTIONS);

        ClientSocket.getInstance().sendJsonRequest(request, "FOLLOWED_AUCTIONS_RESPONSE", response -> {
            // THREAD-SAFETY: Đẩy tác vụ cập nhật UI về luồng chính của JavaFX
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean() && response.has("auctions")) {
                    JsonArray auctions = response.getAsJsonArray("auctions");

                    for (JsonElement element : auctions) {
                        JsonObject obj = element.getAsJsonObject();

                        // Parse an toàn từ JSON DTO
                        int auctionId = obj.has("auctionId") ? obj.get("auctionId").getAsInt() : -1;
                        String name = obj.has("itemName") ? obj.get("itemName").getAsString() : "Sản phẩm";
                        long price = obj.has("currentPrice") ? obj.get("currentPrice").getAsLong() : 0;
                        String status = obj.has("status") ? obj.get("status").getAsString() : "N/A";
                        String imageUrl = obj.has("imageUrl") ? obj.get("imageUrl").getAsString() : "";

                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                            VBox card = loader.load();
                            ProductCardController controller = loader.getController();

                            // Truyền 'true' cho trạng thái isFollowed vì đây là danh sách đã follow
                            controller.setProductData(auctionId, name, price, "Đang diễn ra", status.equals("OPEN") ? "Đang diễn ra" : status, imageUrl, true);
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