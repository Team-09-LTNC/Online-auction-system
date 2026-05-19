package com.auction.client.controller.bidder;

import com.auction.client.controller.auth.UserSession;
import com.auction.client.controller.components.ProductCardController;
import com.auction.client.interfaces.CategoryFilterListener;
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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AuctionListScreenController implements Initializable, CategoryFilterListener {

    private static final Logger logger = LoggerFactory.getLogger(AuctionListScreenController.class);

    @FXML private FlowPane productFlowPane;

    // Khai báo nhãn Header phục vụ cá nhân hóa
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;

    // Trạng thái bộ lọc danh mục
    private String currentCategory = "Tất cả";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Đồng bộ người dùng thật
        updateHeaderUserInfo();

        // 2. Fetch toàn bộ danh sách phiên đấu giá
        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadAuctionsFromServer();
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
            logger.error("Lỗi cập nhật Header tại AuctionListScreen: {}", e.getMessage());
        }
    }

    // Lắng nghe sự kiện click bộ lọc danh mục từ Main UI
    @Override
    public void onCategorySelected(String category) {
        this.currentCategory = category;
        logger.info("Đã chọn danh mục lọc: {}", category);

        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadAuctionsFromServer();
        }
    }

    private void loadAuctionsFromServer() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_ALL_AUCTIONS);

        // Gửi kèm trạng thái lọc lên Server (nếu Server hỗ trợ truy vấn lọc)
        request.addProperty("category", currentCategory);

        ClientSocket.getInstance().sendJsonRequest(request, "ALL_AUCTIONS_RESPONSE", response -> {

            // THREAD-SAFETY: Đảm bảo JavaFX UI Thread xử lý Render
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean() && response.has("auctions")) {
                    JsonArray auctions = response.getAsJsonArray("auctions");

                    // Trích xuất tập ID mà User hiện tại đang follow
                    List<Integer> followedIds = new ArrayList<>();
                    if (response.has("followedIds")) {
                        for (JsonElement el : response.getAsJsonArray("followedIds")) {
                            followedIds.add(el.getAsInt());
                        }
                    }

                    for (JsonElement element : auctions) {
                        JsonObject obj = element.getAsJsonObject();

                        // Lọc phía Client (trong trường hợp Server trả toàn bộ)
                        String itemCategory = obj.has("category") ? obj.get("category").getAsString() : "";
                        if (!"Tất cả".equals(currentCategory) && !currentCategory.equalsIgnoreCase(itemCategory)) {
                            continue;
                        }

                        int auctionId = obj.has("auctionId") ? obj.get("auctionId").getAsInt() : -1;
                        String name = obj.has("itemName") ? obj.get("itemName").getAsString() : "Đang cập nhật";
                        long price = obj.has("currentPrice") ? obj.get("currentPrice").getAsLong() : 0;
                        String status = obj.has("status") ? obj.get("status").getAsString() : "N/A";
                        String imageUrl = obj.has("imageUrl") ? obj.get("imageUrl").getAsString() : "";

                        boolean isFollowed = followedIds.contains(auctionId);

                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                            VBox card = loader.load();
                            ProductCardController controller = loader.getController();

                            // Inject Dữ liệu vào Card
                            controller.setProductData(auctionId, name, price, "Đang diễn ra", status.equals("OPEN") ? "Đang diễn ra" : status, imageUrl, isFollowed);
                            productFlowPane.getChildren().add(card);
                        } catch (IOException e) {
                            logger.error("Không nạp được giao diện ProductCard.fxml: {}", e.getMessage());
                        }
                    }
                }
            });
        });
    }
}