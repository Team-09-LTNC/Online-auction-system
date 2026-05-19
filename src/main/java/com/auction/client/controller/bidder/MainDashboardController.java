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

public class MainDashboardController implements Initializable, com.auction.client.interfaces.CategoryFilterListener {

    private static final Logger logger = LoggerFactory.getLogger(MainDashboardController.class);

    @FXML private FlowPane productFlowPane;

    // 🔥 BIẾN UI: Tên và lời chào dynamic
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private Label lblBannerWelcome;

    // 🔥 BIẾN UI BỔ SUNG: 4 ô số liệu thống kê động
    @FXML private Label lblActiveAuctions;
    @FXML private Label lblEndingSoonAuctions;
    @FXML private Label lblFollowedAuctions;
    @FXML private Label lblMyBidsCount;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Bidder đã vào Dashboard chính - Đang nạp danh sách sản phẩm.");

        // Xóa sạch các card cũ (nếu có) trước khi nạp mới
        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadProductsFromServer();
        }

        // 🔥 CẬP NHẬT TÊN USER ĐĂNG NHẬP
        updateDashboardUserInfo();

        // 🔥 CẬP NHẬT 4 Ô SỐ LIỆU THỐNG KÊ ĐỘNG TỪ SERVER
        updateStatistics();
    }

    private void updateDashboardUserInfo() {
        try {
            String currentUserName = UserSession.getUsername() != null ? UserSession.getUsername() : "Người dùng";
            String currentUserRole = UserSession.getCurrentRole() != null ? UserSession.getCurrentRole() : "BIDDER";

            if (lblHeaderName != null) lblHeaderName.setText("Chào, " + currentUserName);
            if (lblBannerWelcome != null) lblBannerWelcome.setText("Chào mừng trở lại, " + currentUserName + "! 👋");

            if (lblHeaderRole != null) {
                if ("BIDDER".equalsIgnoreCase(currentUserRole)) {
                    lblHeaderRole.setText("Bidder");
                } else if ("SELLER".equalsIgnoreCase(currentUserRole)) {
                    lblHeaderRole.setText("Seller");
                } else {
                    lblHeaderRole.setText(currentUserRole);
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi khi load thông tin User lên Header: {}", e.getMessage());
        }
    }

    // =========================================================================
    // 🔥LOGIC ĐỔ SỐ LIỆU THỐNG KÊ ĐỘNG TỪ DATABASE THÔNG QUA SOCKET
    // =========================================================================
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
                } else {
                    logger.error("Không lấy được thống kê Dashboard: {}", response);
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
                    
                    int count = 0;
                    for (JsonElement element : auctions) {
                        if (count >= 6) break; // Chỉ hiển thị 6 sản phẩm nổi bật nhất ở Trang chủ
                        
                        JsonObject obj = element.getAsJsonObject();
                        int auctionId = obj.has("auctionId") ? obj.get("auctionId").getAsInt() : -1;
                        String name = obj.has("itemName") ? obj.get("itemName").getAsString() : "Sản phẩm";
                        long price = obj.has("currentPrice") ? obj.get("currentPrice").getAsLong() : 0;
                        String status = obj.has("status") ? obj.get("status").getAsString() : "N/A";
                        String imageUrl = obj.has("imageUrl") ? obj.get("imageUrl").getAsString() : "";
                        
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                            VBox card = loader.load();
                            ProductCardController controller = loader.getController();
                            
                            controller.setProductData(auctionId, name, price, "Đang diễn ra", status.equals("OPEN") ? "Đang diễn ra" : status, imageUrl);
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

    @Override
    public void onCategorySelected(String category) {
        System.out.println("LOG: Dashboard đang thực hiện lọc cho danh mục: " + category);
        productFlowPane.getChildren().clear();
        loadProductsFromServer();
    }
}