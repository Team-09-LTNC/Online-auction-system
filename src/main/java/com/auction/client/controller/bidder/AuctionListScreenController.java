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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class AuctionListScreenController implements Initializable, com.auction.client.interfaces.CategoryFilterListener {

    private static final Logger logger = LoggerFactory.getLogger(AuctionListScreenController.class);

    @FXML private FlowPane productFlowPane;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;

    private String currentCategory = "Tất cả";

    private static final DateTimeFormatter MULTI_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            // Chấp nhận cả 'T' hoặc dấu cách
            .optionalStart().appendLiteral('T').optionalEnd()
            .optionalStart().appendLiteral(' ').optionalEnd()
            .appendPattern("HH:mm")
            // Chấp nhận có giây hoặc không
            .optionalStart().appendPattern(":ss").optionalEnd()
            // Chấp nhận có hoặc không có phần mili giây (.S, .SSS, .SSSSSS)
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 1, 6, true).optionalEnd()
            .toFormatter();

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
            Platform.runLater(() -> {
                if (response.has("auctions")) {
                    JsonArray auctions = response.getAsJsonArray("auctions");

                    if (productFlowPane != null) {
                        productFlowPane.getChildren().clear();
                    }

                    List<Integer> followedIds = new ArrayList<>();
                    if (response.has("followedIds")) {
                        for (JsonElement el : response.getAsJsonArray("followedIds")) {
                            followedIds.add(el.getAsInt());
                        }
                    }

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

                        // Gọi bộ tính toán giây siêu cấp
                        AuctionSecondsState state = calculateAuctionSecondsState(rawStartTime, rawEndTime, serverStatus);

                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                            VBox card = loader.load();
                            ProductCardController controller = loader.getController();

                            controller.setProductData(auctionId, name, price, state.countdownSeconds, state.finalStatus, imageUrl, isFollowed);
                            productFlowPane.getChildren().add(card);
                        } catch (IOException e) {
                            logger.error("Không nạp được giao diện ProductCard.fxml: {}", e.getMessage());
                        }
                    }
                }
            });
        });
    }

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static ZonedDateTime parseAndSyncTime(String rawTime) {
        System.out.println("DEBUG: Server gửi về: " + rawTime);
        if (rawTime == null || rawTime.trim().isEmpty() || "null".equalsIgnoreCase(rawTime.trim())) {
            return null;
        }

        try {

            String cleanTime = rawTime.trim().replace(" ", "T");

            // Nếu server gửi UTC dạng:
            // 2026-05-21T10:00:00Z
            if (cleanTime.endsWith("Z")) {
                return Instant.parse(cleanTime).atZone(VIETNAM_ZONE);
            }
            // Nếu server gửi có timezone:
            // 2026-05-21T10:00:00+00:00
            try {
                return OffsetDateTime.parse(cleanTime).atZoneSameInstant(VIETNAM_ZONE);
            } catch (Exception ignored) {
            }
            // Nếu server gửi local time:
            // 2026-05-21T10:00:00
            LocalDateTime localDateTime = LocalDateTime.parse(cleanTime);
            return localDateTime.atZone(VIETNAM_ZONE);
        } catch (Exception e) {
            System.out.println("Parse time lỗi: " + e.getMessage());
            return null;
        }
    }
    public static AuctionSecondsState calculateAuctionSecondsState(
            String rawStartTime,
            String rawEndTime,
            String serverStatus
    ) {

        try {
            ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
            ZonedDateTime start = parseAndSyncTime(rawStartTime);
            ZonedDateTime end = parseAndSyncTime(rawEndTime);
            System.out.println("NOW   : " + now);
            System.out.println("START : " + start);
            System.out.println("END   : " + end);
            if (start == null || end == null) {
                return new AuctionSecondsState(0, "FINISHED");
            }
            if (now.isBefore(start)) {
                long seconds = ChronoUnit.SECONDS.between(now, start);
                return new AuctionSecondsState(
                        (int) Math.max(seconds, 0), "OPEN");
            }
            if ((!now.isBefore(start)) && now.isBefore(end)) {
                long seconds = ChronoUnit.SECONDS.between(now, end);
                return new AuctionSecondsState(
                        (int) Math.max(seconds, 0), "RUNNING"
                );
            }
            return new AuctionSecondsState(0, "FINISHED");

        } catch (Exception e) {
            e.printStackTrace();
            return new AuctionSecondsState(0, "FINISHED");
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
}