package com.auction.client.controller.bidder;

import com.auction.client.controller.auth.UserSession;
import com.auction.client.controller.components.ProductCardController;
import com.auction.client.networkclient.ClientSocket;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.fxml.FXML;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AuctionListScreenController implements Initializable, com.auction.client.interfaces.CategoryFilterListener {

    private static final Logger logger = LoggerFactory.getLogger(AuctionListScreenController.class);

    @FXML private FlowPane productFlowPane;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;

    private String currentCategory = "Tất cả";

    // Bộ giải mã thời gian siêu cấp, cân mọi loại định dạng từ DB
    private static final DateTimeFormatter MULTI_FORMATTER = DateTimeFormatter.ofPattern(
            "[yyyy-MM-dd HH:mm:ss.SSSSSS]" +
                    "[yyyy-MM-dd HH:mm:ss.SSS]" +
                    "[yyyy-MM-dd HH:mm:ss.S]" +
                    "[yyyy-MM-dd HH:mm:ss]" +
                    "[yyyy-MM-dd'T'HH:mm:ss.SSSSSS]" +
                    "[yyyy-MM-dd'T'HH:mm:ss.SSS]" +
                    "[yyyy-MM-dd'T'HH:mm:ss.S]" +
                    "[yyyy-MM-dd'T'HH:mm:ss]"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateHeaderUserInfo();
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

    @Override
    public void onCategorySelected(String category) {
        this.currentCategory = category;
        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadAuctionsFromServer();
        }
    }

    private void loadAuctionsFromServer() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_ALL_AUCTIONS);
        request.addProperty("category", currentCategory);

        ClientSocket.getInstance().sendJsonRequest(request, "AUCTION_LIST_RESPONSE", response -> {
            try {
                if (response.has("auctions")) {
                    JsonArray auctions = response.getAsJsonArray("auctions");
                        List<Integer> followedIds = new ArrayList<>();
                        if (response.has("followedIds")) {
                            for (JsonElement el : response.getAsJsonArray("followedIds")) {
                                followedIds.add(el.getAsInt());
                            }
                        }

                        // Bộ sưu tập tạm thời chứa các Card đã dựng xong xuôi trong luồng ngầm
                        List<VBox> cardsToRender = new ArrayList<>();

                        for (JsonElement element : auctions) {
                            JsonObject obj = element.getAsJsonObject();

                            String itemCategory = obj.has("category") ? obj.get("category").getAsString() : "";
                            if (!"Tất cả".equals(currentCategory) && !currentCategory.equalsIgnoreCase(itemCategory)) {
                                continue;
                            }

                            int auctionId = obj.has("auctionId") ? obj.get("auctionId").getAsInt() : -1;
                            String name = obj.has("itemName") ? obj.get("itemName").getAsString() : "Đang cập nhật";
                            long price = obj.has("currentPrice") ? obj.get("currentPrice").getAsLong() : 0;
                            String imageUrl = obj.has("imageUrl") ? obj.get("imageUrl").getAsString() : "";
                            boolean isFollowed = followedIds.contains(auctionId);

                            String rawStartTime = null;
                            if (obj.has("startTime") && !obj.get("startTime").isJsonNull()) rawStartTime = obj.get("startTime").getAsString();
                            else if (obj.has("start_time") && !obj.get("start_time").isJsonNull()) rawStartTime = obj.get("start_time").getAsString();

                            String rawEndTime = null;
                            if (obj.has("endTime") && !obj.get("endTime").isJsonNull()) rawEndTime = obj.get("endTime").getAsString();
                            else if (obj.has("end_time") && !obj.get("end_time").isJsonNull()) rawEndTime = obj.get("end_time").getAsString();

                            String serverStatus = obj.has("status") ? obj.get("status").getAsString() : "RUNNING";

                            AuctionSecondsState state = calculateAuctionSecondsState(rawStartTime, rawEndTime);

                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                                VBox card = loader.load();
                                ProductCardController controller = loader.getController();

                                controller.setProductData(auctionId, name, price, state.countdownSeconds, state.finalStatus, imageUrl, isFollowed);

                                cardsToRender.add(card);
                            } catch (IOException e) {
                                logger.error("Không nạp được giao diện ProductCard.fxml: {}", e.getMessage());
                            }
                        }

                        // CHỈ DÙNG PLATFORM.RUNLATER KHI GỌI LỆNH ADDALL LÊN MÀN HÌNH CHÍNH
                        Platform.runLater(() -> {
                            if (productFlowPane != null) {
                                productFlowPane.getChildren().clear();
                                productFlowPane.getChildren().addAll(cardsToRender);
                            }
                        });
                    }
                } catch (Exception ex) {
                    logger.error("Lỗi xử lý dựng card sảnh đấu giá trong Thread phụ: ", ex);
                }
            });
    }

    /**
     * Bỏ qua Server, Client tự cầm cân nảy mực dựa vào mốc thời gian thực.
     */

    public static AuctionSecondsState calculateAuctionSecondsState(
            String rawStartTime,
            String rawEndTime
    ) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = parseTime(rawStartTime);
            LocalDateTime end = parseTime(rawEndTime);

            if (start != null && end != null) {
                // Chưa bắt đầu
                if (now.isBefore(start)) {
                    int seconds = (int) ChronoUnit.SECONDS.between(now, start);
                    return new AuctionSecondsState(seconds, "OPEN"
                    );
                }

                // Đang diễn ra
                if ((!now.isBefore(start)) && now.isBefore(end)) {
                    int seconds = (int) ChronoUnit.SECONDS.between(now, end);

                    return new AuctionSecondsState(seconds, "RUNNING"
                    );
                }
            }

            return new AuctionSecondsState(
                    0,
                    "FINISHED"
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new AuctionSecondsState(0, "FINISHED"
            );
        }
    }

    private static LocalDateTime parseTime(String rawTime) {
        if (rawTime == null || rawTime.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(
                    rawTime.trim().replace(" ", "T"),
                    MULTI_FORMATTER
            );
        } catch (Exception e) {
            logger.error("Parse time lỗi: {}", rawTime);
            return null;
        }
    }

    public static class AuctionSecondsState {
        public final int countdownSeconds;
        public final String finalStatus;

        public AuctionSecondsState(int countdownSeconds, String finalStatus) {
            this.countdownSeconds = countdownSeconds;
            this.finalStatus = finalStatus;
        }
    }
    public void refreshData() {
        if (productFlowPane != null) {
            // Xóa rỗng list cũ, hiển thị trạng thái đang tải (nếu muốn)
            productFlowPane.getChildren().clear();
            // Gọi lại API lấy dữ liệu mới
            loadAuctionsFromServer();
        }
    }
}