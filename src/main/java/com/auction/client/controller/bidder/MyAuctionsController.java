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
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

public class MyAuctionsController implements Initializable, RefreshableCenterContent {
    private static final Logger logger = LoggerFactory.getLogger(MyAuctionsController.class);
    private static final int CARD_BATCH_SIZE = 10;

    @FXML
    private FlowPane productFlowPane;
    @FXML
    private Label lblHeaderName;
    @FXML
    private Label lblHeaderRole;
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cbStatus;

    private String currentKeyword = "";
    private String currentStatus = "Tất cả";
    private JsonArray joinedAuctions = new JsonArray();
    private List<Integer> loadedFollowedIds = new ArrayList<>();
    private String loadedServerNow;
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(180));
    private final AtomicInteger renderVersion = new AtomicInteger();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateHeaderUserInfo();

        if (cbStatus != null) {
            cbStatus.getItems().addAll("Tất cả", "OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED");
            cbStatus.setValue("Tất cả");
            cbStatus.valueProperty().addListener((obs, oldVal, newVal) -> onStatusFilterChanged(newVal));
        }

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> onSearchKeywordChanged(newVal));
        }

        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadJoinedAuctionsFromServer();
        }
    }

    private void updateHeaderUserInfo() {
        try {
            String currentUserName = UserSession.getUsername() != null ? UserSession.getUsername() : "Người dùng";
            String currentUserRole = UserSession.getCurrentRole() != null ? UserSession.getCurrentRole() : "BIDDER";

            if (lblHeaderName != null) lblHeaderName.setText("Chào, " + currentUserName);
            if (lblHeaderRole != null) {
                lblHeaderRole.setText(currentUserRole.substring(0, 1).toUpperCase() + currentUserRole.substring(1).toLowerCase());
            }
        } catch (Exception e) {
            logger.error("Lỗi cập nhật thông tin Header tại MyAuctions: {}", e.getMessage());
        }
    }

    private void loadJoinedAuctionsFromServer() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_JOINED_AUCTIONS);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(request, "JOINED_AUCTIONS_RESPONSE", response -> {
            if (!response.has("auctions") || !response.get("auctions").isJsonArray()) {
                return;
            }

            joinedAuctions = response.getAsJsonArray("auctions");
            loadedServerNow = getString(response, "serverNow", null);
            loadedFollowedIds.clear();
            if (response.has("followedIds") && response.get("followedIds").isJsonArray()) {
                for (JsonElement id : response.getAsJsonArray("followedIds")) {
                    loadedFollowedIds.add(id.getAsInt());
                }
            }
            applyFiltersAndRender();
        });
    }

    private void applyFiltersAndRender() {
        int currentRenderVersion = renderVersion.incrementAndGet();
        String selectedKeyword = currentKeyword.trim().toLowerCase(Locale.ROOT);
        String selectedStatus = currentStatus;
        String selectedServerNow = loadedServerNow;
        JsonArray auctionsToFilter = joinedAuctions.deepCopy();
        List<Integer> followedIds = new ArrayList<>(loadedFollowedIds);

        new Thread(() -> {
            try {
                List<VBox> cardsToRender = new ArrayList<>();
                boolean cardsPublished = false;

                for (JsonElement element : auctionsToFilter) {
                    JsonObject obj = element.getAsJsonObject();
                    String name = getString(obj, "itemName", "Đang cập nhật");
                    if (!selectedKeyword.isEmpty()
                            && !name.toLowerCase(Locale.ROOT).contains(selectedKeyword)) {
                        continue;
                    }

                    String startTime = getTime(obj, "startTime", "start_time");
                    String endTime = getTime(obj, "endTime", "end_time");
                    AuctionTimeUtil.AuctionState state =
                            AuctionTimeUtil.calculateState(startTime, endTime, selectedServerNow);
                    String statusForUi = resolveDisplayStatus(getString(obj, "status", null), state.finalStatus);
                    if (!"Tất cả".equals(selectedStatus) && !selectedStatus.equalsIgnoreCase(statusForUi)) {
                        continue;
                    }

                    int auctionId = obj.has("auctionId") ? obj.get("auctionId").getAsInt() : -1;
                    long price = obj.has("currentPrice") ? obj.get("currentPrice").getAsLong() : 0;
                    String imageUrl = getString(obj, "imageUrl", "");
                    com.auction.client.util.ImageCacheManager.preloadPreviewImage(imageUrl);

                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                        VBox card = loader.load();
                        ProductCardController controller = loader.getController();
                        controller.setProductData(
                                auctionId,
                                name,
                                price,
                                state.countdownSeconds,
                                statusForUi,
                                imageUrl,
                                followedIds.contains(auctionId)
                        );
                        JsonObject roomSnapshot = obj.deepCopy();
                        roomSnapshot.addProperty("displayStatus", statusForUi);
                        roomSnapshot.addProperty("countdownSeconds", state.countdownSeconds);
                        if (selectedServerNow != null) {
                            roomSnapshot.addProperty("serverNow", selectedServerNow);
                        }
                        controller.setAuctionSnapshot(roomSnapshot);
                        cardsToRender.add(card);
                        if (cardsToRender.size() >= CARD_BATCH_SIZE) {
                            publishCardBatch(currentRenderVersion, cardsToRender, !cardsPublished);
                            cardsPublished = true;
                            cardsToRender = new ArrayList<>();
                        }
                    } catch (IOException e) {
                        logger.error("Không nạp được ProductCard cho phiên đã tham gia: {}", e.getMessage());
                    }
                }

                publishCardBatch(currentRenderVersion, cardsToRender, !cardsPublished);
            } catch (Exception ex) {
                logger.error("Lỗi dựng danh sách phiên đấu giá đã tham gia", ex);
            }
        }).start();
    }

    private void onSearchKeywordChanged(String newKeyword) {
        currentKeyword = newKeyword == null ? "" : newKeyword;
        searchDebounce.stop();
        searchDebounce.setOnFinished(event -> applyFiltersAndRender());
        searchDebounce.playFromStart();
    }

    private void onStatusFilterChanged(String newStatus) {
        currentStatus = isBlank(newStatus) ? "Tất cả" : newStatus;
        applyFiltersAndRender();
    }

    private String getTime(JsonObject obj, String primaryKey, String fallbackKey) {
        String raw = getString(obj, primaryKey, null);
        return raw != null ? raw : getString(obj, fallbackKey, null);
    }

    private String getString(JsonObject obj, String key, String fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString()
                : fallback;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolveDisplayStatus(String storedStatus, String timeStatus) {
        if (storedStatus == null || storedStatus.isBlank()) {
            return timeStatus;
        }

        String normalized = storedStatus.trim().toUpperCase(Locale.ROOT);
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

    private void publishCardBatch(int currentRenderVersion, List<VBox> cards, boolean replaceExisting) {
        List<VBox> batch = new ArrayList<>(cards);
        Platform.runLater(() -> {
            if (productFlowPane == null || renderVersion.get() != currentRenderVersion) {
                return;
            }

            if (replaceExisting) {
                productFlowPane.getChildren().setAll(batch);
            } else if (!batch.isEmpty()) {
                productFlowPane.getChildren().addAll(batch);
            }
        });
    }

    @Override
    public void refreshContent() {
        loadJoinedAuctionsFromServer();
    }
}
