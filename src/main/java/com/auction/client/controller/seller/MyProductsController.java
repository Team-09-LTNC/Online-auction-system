package com.auction.client.controller.seller;

import com.auction.client.controller.auth.UserSession;
import com.auction.client.controller.components.ProductCardController;
import com.auction.client.networkclient.ClientSocket;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

public class MyProductsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MyProductsController.class);

    @FXML private FlowPane productFlowPane;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private Label lblBannerWelcome;

    // Bộ formatter Lazy-Load chấp hết mọi định dạng lỗi chuỗi nano của MySQL
    private static final DateTimeFormatter MYSQL_LAZY_FORMATTER = DateTimeFormatter.ofPattern(
            "[yyyy-MM-dd HH:mm:ss[.S]][yyyy-MM-dd HH:mm:ss][yyyy-MM-dd'T'HH:mm:ss[.SSS]]"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Người bán đang xem danh sách sản phẩm của chính mình.");
        updateDashboardUserInfo();

        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
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
        Label loadingLabel = new Label("Đang lấy dữ liệu sản phẩm từ máy chủ...");
        loadingLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px; -fx-padding: 20px;");
        productFlowPane.getChildren().add(loadingLabel);

        JsonObject reqJson = new JsonObject();
        reqJson.addProperty("type", ActionType.GET_MY_PRODUCTS);
        reqJson.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(reqJson, ActionType.GET_MY_PRODUCTS, response -> {
            try {
                boolean success = response.has("success") && response.get("success").getAsBoolean();
                if (success && response.has("data")) {
                    Gson gson = new Gson();
                    JsonObject[] items = gson.fromJson(response.get("data"), JsonObject[].class);

                    Platform.runLater(() -> {
                        productFlowPane.getChildren().clear();

                        if (items.length == 0) {
                            productFlowPane.getChildren().add(new Label("Bạn chưa đăng sản phẩm nào."));
                            return;
                        }

                        for (JsonObject itemObj : items) {
                            try {
                                int id = itemObj.has("id") ? itemObj.get("id").getAsInt() : -1;
                                String name = itemObj.has("name") ? itemObj.get("name").getAsString() : "Sản phẩm không tên";
                                double startingPrice = itemObj.has("startingPrice") ? itemObj.get("startingPrice").getAsDouble() : 0.0;
                                String imageUrl = itemObj.has("imageUrl") ? itemObj.get("imageUrl").getAsString() : "";

                                // Đọc trạng thái từ Server trả về
                                String status = itemObj.has("status") ? itemObj.get("status").getAsString() : "RUNNING";

                                // Bốc tách đồng bộ chuỗi an toàn
                                String startTimeStr = itemObj.has("startTime") && !itemObj.get("startTime").isJsonNull() ? itemObj.get("startTime").getAsString() : null;
                                String endTimeStr = itemObj.has("endTime") && !itemObj.get("endTime").isJsonNull() ? itemObj.get("endTime").getAsString() : null;

                                if (endTimeStr == null && itemObj.has("endTimeStr") && !itemObj.get("endTimeStr").isJsonNull()) {
                                    endTimeStr = itemObj.get("endTimeStr").getAsString();
                                }

                                int countdownSeconds = calculateCountdownSeconds(startTimeStr, endTimeStr, status);

                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                                VBox card = loader.load();
                                ProductCardController controller = loader.getController();

                                controller.setProductData(id, name, startingPrice, countdownSeconds, status, imageUrl, false);

                                productFlowPane.getChildren().add(card);
                            } catch (IOException e) {
                                logger.error("Lỗi vẽ thẻ sản phẩm: {}", e.getMessage());
                            }
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        productFlowPane.getChildren().clear();
                        productFlowPane.getChildren().add(new Label("Lỗi: Không thể tải dữ liệu từ Server."));
                    });
                }
            } catch (Exception ex) {
                logger.error("Lỗi phân tích dữ liệu JSON mạng: ", ex);
            }
        });
    }

    private int calculateCountdownSeconds(String startTimeRaw, String endTimeRaw, String status) {
        try {
            LocalDateTime now = LocalDateTime.now();

            if ("OPEN".equalsIgnoreCase(status) && startTimeRaw != null && !startTimeRaw.trim().isEmpty()) {
                String cleanStart = startTimeRaw.trim().replace("T", " ");
                LocalDateTime start = LocalDateTime.parse(cleanStart, MYSQL_LAZY_FORMATTER);
                long diff = ChronoUnit.SECONDS.between(now, start);
                return diff > 0 ? (int) diff : 0;

            } else if (endTimeRaw != null && !endTimeRaw.trim().isEmpty()) {
                String cleanEnd = endTimeRaw.trim().replace("T", " ");
                LocalDateTime end = LocalDateTime.parse(cleanEnd, MYSQL_LAZY_FORMATTER);
                long diff = ChronoUnit.SECONDS.between(now, end);
                return diff > 0 ? (int) diff : 0;
            }
        } catch (Exception e) {
            logger.error("❌ Lỗi xử lý ngày tháng: " + e.getMessage());
        }
        return "OPEN".equalsIgnoreCase(status) ? 300 : 1800; // Trả về fallback nếu lỗi nặng phá hủy luồng
    }
}