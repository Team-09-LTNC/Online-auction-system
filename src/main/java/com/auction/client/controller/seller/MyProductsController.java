package com.auction.client.controller.seller;

import com.auction.client.controller.auth.UserSession;
import com.auction.client.controller.components.ProductCardController;
import com.auction.client.interfaces.RefreshableCenterContent;
import com.auction.client.networkclient.ClientSocket;
import com.auction.client.util.AuctionTimeUtil;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

public class MyProductsController implements Initializable, RefreshableCenterContent {
    private static final Logger logger = LoggerFactory.getLogger(MyProductsController.class);
    private static final int CARD_BATCH_SIZE = 10;

    @FXML private FlowPane productFlowPane;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private Label lblBannerWelcome;
    @FXML private Label lblTotalProducts;
    @FXML private Label lblRunningAuctions;
    @FXML private Label lblFinishedAuctions;
    @FXML private Label lblCanceledAuctions;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> cbStatus;

    private String currentKeyword = "";
    private String currentCategory = "Tất cả";
    private String currentStatus = "Tất cả";
    private String loadedServerNow;
    private JsonArray loadedProducts = new JsonArray();
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(180));
    private final AtomicInteger renderVersion = new AtomicInteger();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Người bán đang xem danh sách sản phẩm của chính mình.");
        updateDashboardUserInfo();
        setupFilters();

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

    private void setupFilters() {
        if (cbCategory != null) {
            cbCategory.getItems().setAll("Tất cả", "ELECTRONICS", "VEHICLE", "ART", "OTHER");
            cbCategory.setValue("Tất cả");
            cbCategory.valueProperty().addListener((obs, oldVal, newVal) -> {
                currentCategory = MyProductsHelper.isBlank(newVal) ? "Tất cả" : newVal;
                applyFiltersAndRender();
            });
        }

        if (cbStatus != null) {
            cbStatus.getItems().setAll("Tất cả", "OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED");
            cbStatus.setValue("Tất cả");
            cbStatus.valueProperty().addListener((obs, oldVal, newVal) -> {
                currentStatus = MyProductsHelper.isBlank(newVal) ? "Tất cả" : newVal;
                applyFiltersAndRender();
            });
        }

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                currentKeyword = newVal == null ? "" : newVal;
                searchDebounce.stop();
                searchDebounce.setOnFinished(event -> applyFiltersAndRender());
                searchDebounce.playFromStart();
            });
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
                    loadedProducts = response.getAsJsonArray("data");
                    loadedServerNow = MyProductsHelper.getString(response, "serverNow", null);
                    updateStatistics();
                    applyFiltersAndRender();
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

    @Override
    public void refreshContent() {
        if (productFlowPane != null) {
            loadMyPostedProducts();
        }
    }

    private void updateStatistics() {
        int totalProducts = loadedProducts.size();
        int runningCount = 0;
        int finishedCount = 0;
        int canceledCount = 0;

        for (JsonElement element : loadedProducts) {
            JsonObject obj = element.getAsJsonObject();
            AuctionTimeUtil.AuctionState state =
                    AuctionTimeUtil.calculateState(
                            MyProductsHelper.getString(obj, "startTime", null),
                            MyProductsHelper.getString(obj, "endTime", null),
                            loadedServerNow);
            String status = MyProductsHelper.resolveDisplayStatus(
                    MyProductsHelper.getString(obj, "status", null),
                    state.finalStatus
            );
            if ("RUNNING".equalsIgnoreCase(status)) {
                runningCount++;
            } else if ("FINISHED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
                finishedCount++;
            } else if ("CANCELED".equalsIgnoreCase(status)) {
                canceledCount++;
            }
        }

        int finalRunningCount = runningCount;
        int finalFinishedCount = finishedCount;
        int finalCanceledCount = canceledCount;
        Platform.runLater(() -> {
            if (lblTotalProducts != null) lblTotalProducts.setText(String.valueOf(totalProducts));
            if (lblRunningAuctions != null) lblRunningAuctions.setText(String.valueOf(finalRunningCount));
            if (lblFinishedAuctions != null) lblFinishedAuctions.setText(String.valueOf(finalFinishedCount));
            if (lblCanceledAuctions != null) lblCanceledAuctions.setText(String.valueOf(finalCanceledCount));
        });
    }

    private void applyFiltersAndRender() {
        int currentRenderVersion = renderVersion.incrementAndGet();
        JsonArray productsToFilter = loadedProducts;
        String selectedKeyword = currentKeyword.trim().toLowerCase(Locale.ROOT);
        String selectedCategory = currentCategory;
        String selectedStatus = currentStatus;
        String selectedServerNow = loadedServerNow;

        com.auction.client.util.ClientTaskExecutor.execute(() -> {
            try {
                List<VBox> cardsToRender = new ArrayList<>();
                boolean cardsPublished = false;

                for (JsonElement element : productsToFilter) {
                    JsonObject itemObj = element.getAsJsonObject();
                    String name = MyProductsHelper.getString(itemObj, "name", "Sản phẩm không tên");
                    if (!selectedKeyword.isEmpty()
                            && !name.toLowerCase(Locale.ROOT).contains(selectedKeyword)) {
                        continue;
                    }

                    String category = MyProductsHelper.getString(itemObj, "category", "");
                    if (!"Tất cả".equalsIgnoreCase(selectedCategory)
                            && !selectedCategory.equalsIgnoreCase(category)) {
                        continue;
                    }

                    AuctionTimeUtil.AuctionState state =
                            AuctionTimeUtil.calculateState(
                                    MyProductsHelper.getString(itemObj, "startTime", null),
                                    MyProductsHelper.getString(itemObj, "endTime", null),
                                    selectedServerNow);
                    String status = MyProductsHelper.resolveDisplayStatus(
                            MyProductsHelper.getString(itemObj, "status", null),
                            state.finalStatus
                    );
                    boolean statusMatched;
                    if ("Tất cả".equalsIgnoreCase(selectedStatus)) {
                        statusMatched = true;
                    } else if ("FINISHED".equalsIgnoreCase(selectedStatus)) {
                        statusMatched = "FINISHED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status);
                    } else {
                        statusMatched = selectedStatus.equalsIgnoreCase(status);
                    }

                    if (!statusMatched) {
                        continue;
                    }

                    int auctionId = itemObj.has("auctionId") ? itemObj.get("auctionId").getAsInt() : -1;
                    double currentPrice = itemObj.has("currentPrice") ? itemObj.get("currentPrice").getAsDouble() : 0.0;
                    String imageUrl = MyProductsHelper.getString(itemObj, "imageUrl", "");
                    String imageThumbUrl = MyProductsHelper.getString(itemObj, "imageThumbUrl", imageUrl);
                    com.auction.client.util.ImageCacheManager.preloadPreviewImage(imageThumbUrl);

                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                        VBox card = loader.load();
                        ProductCardController controller = loader.getController();
                        controller.setProductData(
                                auctionId,
                                name,
                                currentPrice,
                                state.countdownSeconds,
                                status,
                                imageUrl,
                                imageThumbUrl,
                                false
                        );
                        JsonObject cardData = itemObj.deepCopy();
                        boolean canManage = "OPEN".equalsIgnoreCase(status);
                        controller.configureSellerActions(
                                canManage,
                                () -> showEditDialog(cardData),
                                () -> confirmAndDelete(cardData)
                        );
                        cardsToRender.add(card);
                        if (cardsToRender.size() >= CARD_BATCH_SIZE) {
                            publishCardBatch(currentRenderVersion, cardsToRender, !cardsPublished);
                            cardsPublished = true;
                            cardsToRender = new ArrayList<>();
                        }
                    } catch (IOException e) {
                        logger.error("Lỗi vẽ thẻ sản phẩm: {}", e.getMessage());
                    }
                }

                publishCardBatch(currentRenderVersion, cardsToRender, !cardsPublished);
            } catch (Exception ex) {
                logger.error("Lỗi lọc và dựng sản phẩm seller: ", ex);
            }
        });
    }

    private void publishCardBatch(int currentRenderVersion, List<VBox> cards, boolean replaceExisting) {
        List<VBox> batch = new ArrayList<>(cards);
        Platform.runLater(() -> {
            if (productFlowPane == null || renderVersion.get() != currentRenderVersion) {
                return;
            }

            if (replaceExisting) {
                productFlowPane.getChildren().setAll(batch);
                if (batch.isEmpty()) {
                    productFlowPane.getChildren().add(new Label("Không có sản phẩm phù hợp."));
                }
            } else if (!batch.isEmpty()) {
                productFlowPane.getChildren().addAll(batch);
            }
        });
    }

    private void showEditDialog(JsonObject itemObj) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Sửa sản phẩm");
        dialog.setHeaderText("Chỉ có thể sửa khi phiên đang chờ mở.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField txtName = new TextField(MyProductsHelper.getString(itemObj, "name", ""));
        TextArea txtDescription = new TextArea(MyProductsHelper.getString(itemObj, "description", ""));
        txtDescription.setPrefRowCount(3);
        TextField txtPrice = new TextField(String.valueOf(MyProductsHelper.getLong(itemObj, "startingPrice", 0L)));
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().setAll("ELECTRONICS", "VEHICLE", "ART", "OTHER");
        categoryBox.setValue(MyProductsHelper.getString(itemObj, "category", "OTHER"));
        TextField txtImageUrl = new TextField(MyProductsHelper.getString(itemObj, "imageUrl", ""));
        DatePicker dpStartDate = new DatePicker();
        TextField txtStartTime = new TextField();
        DatePicker dpEndDate = new DatePicker();
        TextField txtEndTime = new TextField();
        MyProductsHelper.fillDateTimeFields(MyProductsHelper.getString(itemObj, "startTime", null), dpStartDate, txtStartTime);
        MyProductsHelper.fillDateTimeFields(MyProductsHelper.getString(itemObj, "endTime", null), dpEndDate, txtEndTime);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Tên"), txtName);
        grid.addRow(1, new Label("Mô tả"), txtDescription);
        grid.addRow(2, new Label("Giá khởi điểm"), txtPrice);
        grid.addRow(3, new Label("Loại"), categoryBox);
        grid.addRow(4, new Label("Ảnh"), txtImageUrl);
        grid.addRow(5, new Label("Ngày bắt đầu"), dpStartDate);
        grid.addRow(6, new Label("Giờ bắt đầu"), txtStartTime);
        grid.addRow(7, new Label("Ngày kết thúc"), dpEndDate);
        grid.addRow(8, new Label("Giờ kết thúc"), txtEndTime);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .ifPresent(button -> submitProductUpdate(
                        itemObj,
                        txtName.getText(),
                        txtDescription.getText(),
                        txtPrice.getText(),
                        categoryBox.getValue(),
                        txtImageUrl.getText(),
                        dpStartDate.getValue(),
                        txtStartTime.getText(),
                        dpEndDate.getValue(),
                        txtEndTime.getText()
                ));
    }

    private void submitProductUpdate(
            JsonObject itemObj,
            String name,
            String description,
            String priceText,
            String category,
            String imageUrl,
            LocalDate startDate,
            String startTimeText,
            LocalDate endDate,
            String endTimeText
    ) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Tên sản phẩm không được để trống.");
            return;
        }

        long startingPrice;
        try {
            startingPrice = Long.parseLong(priceText.replace(",", "").trim());
        } catch (Exception e) {
            showAlert(Alert.AlertType.WARNING, "Giá không hợp lệ", "Giá khởi điểm phải là số nguyên dương.");
            return;
        }

        if (startingPrice <= 0) {
            showAlert(Alert.AlertType.WARNING, "Giá không hợp lệ", "Giá khởi điểm phải lớn hơn 0.");
            return;
        }

        LocalDateTime startTime = MyProductsHelper.parseDateTime(startDate, startTimeText);
        LocalDateTime endTime = MyProductsHelper.parseDateTime(endDate, endTimeText);
        if (startTime == null || endTime == null) {
            showAlert(Alert.AlertType.WARNING, "Thời gian không hợp lệ", "Vui lòng nhập ngày và giờ theo định dạng HH:mm.");
            return;
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            showAlert(Alert.AlertType.WARNING, "Thời gian không hợp lệ", "Thời gian bắt đầu không được ở quá khứ.");
            return;
        }
        if (!endTime.isAfter(startTime)) {
            showAlert(Alert.AlertType.WARNING, "Thời gian không hợp lệ", "Thời gian kết thúc phải lớn hơn thời gian bắt đầu.");
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.UPDATE_PRODUCT);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());
        request.addProperty("itemId", MyProductsHelper.getInt(itemObj, "itemId", MyProductsHelper.getInt(itemObj, "id", -1)));
        request.addProperty("name", trimmedName);
        request.addProperty("description", description == null ? "" : description.trim());
        request.addProperty("startingPrice", startingPrice);
        request.addProperty("category", category == null ? "OTHER" : category);
        request.addProperty("imageUrl", imageUrl == null ? "" : imageUrl.trim());
        request.addProperty("startTime", startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        request.addProperty("endTime", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        ClientSocket.getInstance().sendJsonRequest(request, ActionType.UPDATE_PRODUCT, response ->
                Platform.runLater(() -> handleMutationResponse(response, "Cập nhật sản phẩm thành công.")));
    }

    private void confirmAndDelete(JsonObject itemObj) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xóa sản phẩm");
        confirm.setHeaderText("Xóa sản phẩm đang chờ mở phiên?");
        confirm.setContentText("Thao tác này sẽ hủy đăng bán sản phẩm và không thể hoàn tác.");

        confirm.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .ifPresent(button -> submitProductDelete(itemObj));
    }

    private void submitProductDelete(JsonObject itemObj) {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.DELETE_PRODUCT);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());
        request.addProperty("itemId", MyProductsHelper.getInt(itemObj, "itemId", MyProductsHelper.getInt(itemObj, "id", -1)));

        ClientSocket.getInstance().sendJsonRequest(request, ActionType.DELETE_PRODUCT, response ->
                Platform.runLater(() -> handleMutationResponse(response, "Xóa sản phẩm thành công.")));
    }

    private void handleMutationResponse(JsonObject response, String successMessage) {
        boolean success = response.has("success") && response.get("success").getAsBoolean();
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", successMessage);
            loadMyPostedProducts();
            return;
        }

        showAlert(Alert.AlertType.WARNING, "Không thể thực hiện", MyProductsHelper.getString(response, "message", "Yêu cầu bị từ chối."));
    }


    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}
