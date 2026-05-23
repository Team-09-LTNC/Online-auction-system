package com.auction.client.controller.bidder;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.client.controller.auth.UserSession;
import com.auction.client.controller.components.ProductCardController;
import com.auction.client.networkclient.ClientSocket;
import com.auction.common.enums.ActionType;
import com.auction.client.util.AuctionTimeUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class AuctionListScreenController
        implements Initializable, com.auction.client.interfaces.CategoryFilterListener {

    private static final Logger logger = LoggerFactory.getLogger(AuctionListScreenController.class);

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

    private String currentCategory = "ALL";

    // --- THÊM BIẾN LƯU TRỮ TRẠNG THÁI BỘ LỌC ---
    private String currentKeyword = "";
    private String currentStatus = "Tất cả"; // OPEN, RUNNING, FINISHED, PAID, CANCELLED

    private JsonArray allLoadedAuctions = new JsonArray();
    private List<Integer> loadedFollowedIds = new ArrayList<>();
    private String serverNow;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateHeaderUserInfo();

        // --- KHỞI TẠO COMBOBOX TRẠNG THÁI (LỌC) ---
        if (cbStatus != null) {
            // Nạp các lựa chọn vào ComboBox
            cbStatus.getItems().addAll("Tất cả", "OPEN", "RUNNING", "FINISHED", "PAID", "CANCELLED");
            cbStatus.setValue("Tất cả"); // Giá trị mặc định

            // Lắng nghe sự kiện khi người dùng chọn 1 trạng thái mới
            cbStatus.valueProperty().addListener((obs, oldVal, newVal) -> {
                onStatusFilterChanged(newVal);
            });
        }

        // --- LẮNG NGHE SỰ KIỆN GÕ PHÍM TRÊN THANH TÌM KIẾM ---
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                onSearchKeywordChanged(newVal); // Vừa gõ là giao diện vừa tự động lọc
            });
        }

        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadAuctionsFromServer();
        }
    }

    private void updateHeaderUserInfo() {
        try {
            String currentUserName = UserSession.getUsername() != null ? UserSession.getUsername() : "Người dùng";
            String currentUserRole = UserSession.getCurrentRole() != null ? UserSession.getCurrentRole() : "BIDDER";

            if (lblHeaderName != null)
                lblHeaderName.setText("Chào, " + currentUserName);
            if (lblHeaderRole != null) {
                lblHeaderRole.setText(currentUserRole.substring(0, 1).toUpperCase()
                        + currentUserRole.substring(1).toLowerCase() + " ˅");
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
            if (response.has("auctions")) {
                serverNow = response.has("serverNow")
                        ? response.get("serverNow").getAsString()
                        : null;
                // Lưu toàn bộ dữ liệu gốc vào biến toàn cục để tái sử dụng khi người dùng
                // Filter/Search
                allLoadedAuctions = response.getAsJsonArray("auctions");

                loadedFollowedIds.clear();
                if (response.has("followedIds")) {
                    for (JsonElement el : response.getAsJsonArray("followedIds")) {
                        loadedFollowedIds.add(el.getAsInt());
                    }
                }

                // Gọi hàm lọc và hiển thị
                applyFiltersAndRender();
            }
        });
    }

    /**
     * Hàm lõi xử lý Lọc (Category, Status) và Tìm kiếm (Keyword) trực tiếp trên
     * Client
     */
    private void applyFiltersAndRender() {
        // Chạy trong luồng phụ để không làm đơ giao diện
        new Thread(() -> {
            try {
                List<VBox> cardsToRender = new ArrayList<>();

                for (JsonElement element : allLoadedAuctions) {
                    JsonObject obj = element.getAsJsonObject();

                    System.out.println("CỤC JSON THẬT TỪ SERVER: " + obj.toString());

                    // 1. Lọc theo Danh mục (Category)
                    String itemCategory = obj.has("category") ? obj.get("category").getAsString() : "";
                    if (!"ALL".equals(currentCategory) && !currentCategory.equalsIgnoreCase(itemCategory)) {
                        continue;
                    }

                    // 2. Lọc theo Từ khóa Tìm kiếm (Keyword)
                    String name = obj.has("itemName") ? obj.get("itemName").getAsString() : "Đang cập nhật";
                    if (!currentKeyword.trim().isEmpty()
                            && !name.toLowerCase().contains(currentKeyword.trim().toLowerCase())) {
                        continue;
                    }

                    // Tính toán trạng thái thực tế
                    String rawStartTime = null;
                    if (obj.has("startTime") && !obj.get("startTime").isJsonNull())
                        rawStartTime = obj.get("startTime").getAsString();
                    else if (obj.has("start_time") && !obj.get("start_time").isJsonNull())
                        rawStartTime = obj.get("start_time").getAsString();

                    String rawEndTime = null;
                    if (obj.has("endTime") && !obj.get("endTime").isJsonNull())
                        rawEndTime = obj.get("endTime").getAsString();
                    else if (obj.has("end_time") && !obj.get("end_time").isJsonNull())
                        rawEndTime = obj.get("end_time").getAsString();

                    AuctionTimeUtil.AuctionState state =
                            AuctionTimeUtil.calculateState(rawStartTime, rawEndTime, serverNow);

                    String serverStatus = obj.has("status")
                            ? obj.get("status").getAsString()
                            : state.finalStatus;

                    String effectiveStatus =
                            (serverStatus.equals("PAID") || serverStatus.equals("CANCELLED"))
                                    ? serverStatus
                                    : state.finalStatus;

                    // 3. Lọc theo Trạng thái (Status: OPEN, RUNNING, FINISHED, vv..)
                    if (!"Tất cả".equals(currentStatus) && !currentStatus.equalsIgnoreCase(effectiveStatus)) {
                        continue;
                    }

                    // Dựng Card nếu thỏa mãn toàn bộ bộ lọc
                    int auctionId = obj.has("auctionId") ? obj.get("auctionId").getAsInt() : -1;
                    long price = obj.has("currentPrice") ? obj.get("currentPrice").getAsLong() : 0;
                    String imageUrl = obj.has("imageUrl") ? obj.get("imageUrl").getAsString() : "";
                    boolean isFollowed = loadedFollowedIds.contains(auctionId);

                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                        VBox card = loader.load();
                        ProductCardController controller = loader.getController();

                        controller.setProductData(auctionId, name, price, state.countdownSeconds, effectiveStatus,
                                imageUrl, isFollowed);

                        cardsToRender.add(card);
                    } catch (IOException e) {
                        logger.error("Không nạp được giao diện ProductCard.fxml: {}", e.getMessage());
                    }
                }

                // Cập nhật lên UI
                Platform.runLater(() -> {
                    if (productFlowPane != null) {
                        productFlowPane.getChildren().clear();
                        productFlowPane.getChildren().addAll(cardsToRender);
                    }
                });
            } catch (Exception ex) {
                logger.error("Lỗi xử lý dựng card sảnh đấu giá trong Thread phụ: ", ex);
            }
        }).start();
    }

    public void refreshData() {
        if (productFlowPane != null) {
            // Xóa rỗng list cũ, hiển thị trạng thái đang tải (nếu muốn)
            productFlowPane.getChildren().clear();
            // Gọi lại API lấy dữ liệu mới
            loadAuctionsFromServer();
        }
    }


    /**
     * Gọi hàm này từ sự kiện onKeyTyped của ô tìm kiếm
     */
    public void onSearchKeywordChanged(String newKeyword) {
        this.currentKeyword = newKeyword;
        applyFiltersAndRender(); // Lọc lại trên dữ liệu hiện có
    }

    /**
     * Gọi hàm này từ sự kiện onAction của ComboBox trạng thái
     */
    public void onStatusFilterChanged(String newStatus) {
        this.currentStatus = newStatus;
        applyFiltersAndRender(); // Lọc lại trên dữ liệu hiện có
    }
}