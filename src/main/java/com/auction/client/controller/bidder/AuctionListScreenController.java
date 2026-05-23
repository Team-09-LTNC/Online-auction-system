package com.auction.client.controller.bidder;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class AuctionListScreenController
        implements Initializable,
        com.auction.client.interfaces.CategoryFilterListener,
        RefreshableCenterContent {

    private static final Logger logger =
            LoggerFactory.getLogger(AuctionListScreenController.class);

    private static final int CARD_BATCH_SIZE = 10;

    @FXML private FlowPane productFlowPane;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbStatus;

    private String currentCategory = "ALL";
    private String currentKeyword = "";
    private String currentStatus = "Tất cả";

    private JsonArray allLoadedAuctions = new JsonArray();
    private List<Integer> loadedFollowedIds = new ArrayList<>();
    private String loadedServerNow;

    private final PauseTransition searchDebounce =
            new PauseTransition(Duration.millis(180));

    private final AtomicInteger renderVersion =
            new AtomicInteger();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateHeaderUserInfo();
        setupStatusFilter();
        setupSearchBox();

        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadAuctionsFromServer();
        }
    }

    private void updateHeaderUserInfo() {
        try {
            String currentUserName = UserSession.getUsername() != null ? UserSession.getUsername() : "Người dùng";

            String currentUserRole = UserSession.getCurrentRole() != null ? UserSession.getCurrentRole() : "BIDDER";

            if (lblHeaderName != null) {
                lblHeaderName.setText("Chào, " + currentUserName);
            }

            if (lblHeaderRole != null) {
                lblHeaderRole.setText(
                        currentUserRole.substring(0, 1).toUpperCase()
                                + currentUserRole.substring(1).toLowerCase()
                                + " ˅"
                );
            }

        } catch (Exception e) {
            logger.error("Lỗi cập nhật Header tại AuctionListScreen", e);
        }
    }

    private void setupStatusFilter() {
        if (cbStatus == null) return;

        cbStatus.getItems().setAll(
                "Tất cả",
                "OPEN",
                "RUNNING",
                "FINISHED",
                "PAID",
                "CANCELLED"
        );

        cbStatus.setValue("Tất cả");

        cbStatus.valueProperty().addListener((obs, oldVal, newVal) -> {
            onStatusFilterChanged(newVal);
        });
    }

    private void setupSearchBox() {
        if (txtSearch == null) return;

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            onSearchKeywordChanged(newVal);
        });
    }

    @Override
    public void onCategorySelected(String category) {
        this.currentCategory = isBlank(category) ? "ALL" : category;
        applyFiltersAndRender();
    }

    private void loadAuctionsFromServer() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_ALL_AUCTIONS);

        // Lấy ALL một lần, sau đó lọc local để search/category/status mượt hơn.
        request.addProperty("category", "ALL");
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(
                request,
                "AUCTION_LIST_RESPONSE",
                response -> {
                    if (response.has("auctions")
                            && response.get("auctions").isJsonArray()) {

                        allLoadedAuctions =
                                response.getAsJsonArray("auctions");

                        loadedServerNow =
                                getString(response, "serverNow", null);

                        loadedFollowedIds.clear();

                        if (response.has("followedIds")
                                && response.get("followedIds").isJsonArray()) {

                            for (JsonElement el :
                                    response.getAsJsonArray("followedIds")) {

                                loadedFollowedIds.add(el.getAsInt());
                            }
                        }

                        applyFiltersAndRender();
                    }
                }
        );
    }

    private void applyFiltersAndRender() {
        int currentRenderVersion =
                renderVersion.incrementAndGet();

        String selectedCategory =
                currentCategory;

        String selectedKeyword =
                currentKeyword.trim().toLowerCase(Locale.ROOT);

        String selectedStatus =
                currentStatus;

        String selectedServerNow =
                loadedServerNow;

        JsonArray auctionsToFilter =
                allLoadedAuctions.deepCopy();

        List<Integer> followedIds =
                new ArrayList<>(loadedFollowedIds);

        new Thread(() -> {
            try {
                List<VBox> cardsToRender = new ArrayList<>();
                boolean cardsPublished = false;

                for (JsonElement element : auctionsToFilter) {
                    JsonObject obj = element.getAsJsonObject();

                    String itemCategory =
                            getString(obj, "category", "");

                    if (!"ALL".equalsIgnoreCase(selectedCategory)
                            && !selectedCategory.equalsIgnoreCase(itemCategory)) {
                        continue;
                    }

                    String name =
                            getString(obj, "itemName", "Đang cập nhật");

                    if (!selectedKeyword.isEmpty()
                            && !name.toLowerCase(Locale.ROOT)
                            .contains(selectedKeyword)) {
                        continue;
                    }

                    String rawStartTime =
                            getTime(obj, "startTime", "start_time");

                    String rawEndTime =
                            getTime(obj, "endTime", "end_time");

                    AuctionTimeUtil.AuctionState state =
                            AuctionTimeUtil.calculateState(
                                    rawStartTime,
                                    rawEndTime,
                                    selectedServerNow
                            );

                    String serverStatus =
                            getString(obj, "status", state.finalStatus);

                    String effectiveStatus =
                            getEffectiveStatus(serverStatus, state.finalStatus);

                    if (!"Tất cả".equalsIgnoreCase(selectedStatus)
                            && !selectedStatus.equalsIgnoreCase(effectiveStatus)) {
                        continue;
                    }

                    int auctionId =
                            obj.has("auctionId")
                                    ? obj.get("auctionId").getAsInt()
                                    : -1;

                    long price =
                            obj.has("currentPrice")
                                    ? obj.get("currentPrice").getAsLong()
                                    : 0;

                    String imageUrl =
                            getString(obj, "imageUrl", "");

                    boolean isFollowed =
                            followedIds.contains(auctionId);

                    com.auction.client.util.ImageCacheManager
                            .preloadPreviewImage(imageUrl);

                    try {
                        FXMLLoader loader =
                                new FXMLLoader(
                                        getClass().getResource(
                                                "/fxml/components/ProductCard.fxml"
                                        )
                                );

                        VBox card = loader.load();

                        ProductCardController controller =
                                loader.getController();

                        controller.setProductData(
                                auctionId,
                                name,
                                price,
                                state.countdownSeconds,
                                effectiveStatus,
                                imageUrl,
                                isFollowed
                        );

                        cardsToRender.add(card);

                        if (cardsToRender.size() >= CARD_BATCH_SIZE) {
                            publishCardBatch(
                                    currentRenderVersion,
                                    cardsToRender,
                                    !cardsPublished
                            );

                            cardsPublished = true;
                            cardsToRender = new ArrayList<>();
                        }

                    } catch (IOException e) {
                        logger.error(
                                "Không nạp được giao diện ProductCard.fxml",
                                e
                        );
                    }
                }

                publishCardBatch(
                        currentRenderVersion,
                        cardsToRender,
                        !cardsPublished
                );

            } catch (Exception ex) {
                logger.error(
                        "Lỗi xử lý dựng card sảnh đấu giá trong Thread phụ",
                        ex
                );
            }
        }).start();
    }

    public void refreshData() {
        if (productFlowPane != null) {
            loadAuctionsFromServer();
        }
    }

    @Override
    public void refreshContent() {
        refreshData();
    }

    public void onSearchKeywordChanged(String newKeyword) {
        this.currentKeyword = newKeyword == null ? "" : newKeyword;

        searchDebounce.stop();

        searchDebounce.setOnFinished(event -> {
            applyFiltersAndRender();
        });

        searchDebounce.playFromStart();
    }

    public void onStatusFilterChanged(String newStatus) {
        this.currentStatus = isBlank(newStatus) ? "Tất cả" : newStatus;
        applyFiltersAndRender();
    }

    private String getEffectiveStatus(String serverStatus, String calculatedStatus) {
        if (serverStatus == null) {
            return calculatedStatus;
        }

        if ("PAID".equalsIgnoreCase(serverStatus)
                || "CANCELLED".equalsIgnoreCase(serverStatus)
                || "CANCELED".equalsIgnoreCase(serverStatus)) {

            return "CANCELED".equalsIgnoreCase(serverStatus)
                    ? "CANCELLED"
                    : serverStatus;
        }

        return calculatedStatus;
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

    private void publishCardBatch(
            int currentRenderVersion,
            List<VBox> cards,
            boolean replaceExisting
    ) {
        List<VBox> batch = new ArrayList<>(cards);

        Platform.runLater(() -> {
            if (productFlowPane == null
                    || renderVersion.get() != currentRenderVersion) {
                return;
            }

            if (replaceExisting) {
                productFlowPane.getChildren().setAll(batch);
            } else if (!batch.isEmpty()) {
                productFlowPane.getChildren().addAll(batch);
            }
        });
    }
}