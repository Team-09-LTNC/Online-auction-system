package com.auction.client.controller.seller;

import com.auction.client.controller.auth.UserSession;
import com.auction.client.controller.components.ProductCardController;
import com.auction.client.networkclient.ClientSocket;
import com.auction.client.util.AuctionTimeUtil; // IMPORT CÁI NÀY
import com.auction.common.enums.ActionType;
import com.google.gson.Gson;
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
import java.util.ResourceBundle;

public class MyProductsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MyProductsController.class);

    @FXML private FlowPane productFlowPane;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private Label lblBannerWelcome;

    private String serverNow; // Cần biến này để đồng bộ thời gian từ Server

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateDashboardUserInfo();
        if (productFlowPane != null) {
            loadMyPostedProducts();
        }
    }

    private void updateDashboardUserInfo() {
        try {
            String currentUserName = UserSession.getUsername() != null ? UserSession.getUsername() : "Người dùng";
            String currentUserRole = UserSession.getCurrentRole() != null ? UserSession.getCurrentRole() : "SELLER";

            if (lblHeaderName != null) lblHeaderName.setText("Chào, " + currentUserName);
            if (lblBannerWelcome != null) lblBannerWelcome.setText("Chào mừng trở lại, " + currentUserName + "! 👋");
            if (lblHeaderRole != null) {
                lblHeaderRole.setText(currentUserRole.substring(0, 1).toUpperCase() + currentUserRole.substring(1).toLowerCase());
            }
        } catch (Exception e) {
            logger.error("Lỗi tải thông tin cá nhân: {}", e.getMessage());
        }
    }

    private void loadMyPostedProducts() {
        productFlowPane.getChildren().clear();

        JsonObject reqJson = new JsonObject();
        reqJson.addProperty("type", ActionType.GET_MY_PRODUCTS);
        reqJson.addProperty("requestId", System.currentTimeMillis());

        ClientSocket.getInstance().sendJsonRequest(reqJson, ActionType.GET_MY_PRODUCTS, response -> {
            System.out.println("DEBUG JSON SELLER: " + response.toString());
            try {
                // Lấy serverNow từ server để tính toán thời gian chính xác
                serverNow = response.has("serverNow") ? response.get("serverNow").getAsString() : LocalDateTime.now().toString();

                if (response.has("success") && response.get("success").getAsBoolean() && response.has("data")) {
                    Gson gson = new Gson();
                    JsonObject[] items = gson.fromJson(response.get("data"), JsonObject[].class);

                    Platform.runLater(() -> {
                        productFlowPane.getChildren().clear();
                        for (JsonObject itemObj : items) {
                            renderCard(itemObj);
                        }
                    });
                }
            } catch (Exception ex) {
                logger.error("Lỗi load sản phẩm: ", ex);
            }
        });
    }

    private void renderCard(JsonObject itemObj) {
        try {
            System.out.println("DEBUG KEYS: " + itemObj.keySet());
            int id = itemObj.has("auctionId") ? itemObj.get("auctionId").getAsInt() : -1;
            String name = itemObj.has("itemName") ? itemObj.get("itemName").getAsString() : "Sản phẩm không tên";
            double startingPrice = itemObj.has("currentPrice") ? itemObj.get("currentPrice").getAsDouble() : 0.0;
            String imageUrl = itemObj.has("imageUrl") ? itemObj.get("imageUrl").getAsString() : "";

            // --- LOGIC ĐỒNG BỘ ---
            String startTime = itemObj.has("startTime") ? itemObj.get("startTime").getAsString() : null;
            String endTime = itemObj.has("endTime") ? itemObj.get("endTime").getAsString() : null;
            String serverStatus = itemObj.has("status") ? itemObj.get("status").getAsString() : "OPEN";

            // Gọi bộ não AuctionTimeUtil
            AuctionTimeUtil.AuctionState state = AuctionTimeUtil.calculateState(startTime, endTime, serverNow);

            // Xử lý status ưu tiên
            String effectiveStatus = (serverStatus.equals("PAID") || serverStatus.equals("CANCELLED"))
                    ? serverStatus : state.finalStatus;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
            VBox card = loader.load();
            ProductCardController controller = loader.getController();

            // Truyền số giây và status sang Card
            controller.setProductData(id, name, (long)startingPrice, state.countdownSeconds, effectiveStatus, imageUrl, false);

            productFlowPane.getChildren().add(card);
        } catch (IOException e) {
            logger.error("Lỗi vẽ thẻ: {}", e.getMessage());
        }
    }
}