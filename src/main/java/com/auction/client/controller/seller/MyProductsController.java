package com.auction.client.controller.seller;

import com.auction.client.controller.auth.UserSession;
import com.auction.client.interfaces.RefreshableCenterContent;
import com.auction.client.networkclient.ClientSocket;
import com.auction.client.util.AuctionTimeUtil;
import com.auction.client.util.CloudStorageUtil;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

public class MyProductsController implements Initializable, RefreshableCenterContent {
    private static final Logger logger = LoggerFactory.getLogger(MyProductsController.class);

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
    private final MyProductEditDialog editDialog = new MyProductEditDialog();
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(180));
    private final AtomicInteger renderVersion = new AtomicInteger();
    private MyProductsRenderer renderer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Người bán đang xem danh sách sản phẩm của chính mình.");
        updateDashboardUserInfo();
        setupFilters();
        renderer = new MyProductsRenderer(
                getClass(), productFlowPane, renderVersion, this::showEditDialog, this::confirmAndDelete);

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
        renderer.render(loadedProducts, currentKeyword, currentCategory, currentStatus, loadedServerNow);
    }

    private void showEditDialog(JsonObject itemObj) {
        editDialog.show(itemObj, submission -> submitProductUpdate(itemObj, submission));
    }

    private void submitProductUpdate(JsonObject itemObj, MyProductEditDialog.Submission submission) {
        MyProductEditRequest editRequest = submission.request();
        String trimmedName = editRequest.name() == null ? "" : editRequest.name().trim();
        if (trimmedName.isEmpty()) {
            submission.reset();
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Tên sản phẩm không được để trống.");
            return;
        }

        long startingPrice;
        try {
            startingPrice = Long.parseLong(editRequest.priceText().replace(",", "").trim());
        } catch (Exception e) {
            submission.reset();
            showAlert(Alert.AlertType.WARNING, "Giá không hợp lệ", "Giá khởi điểm phải là số nguyên dương.");
            return;
        }

        if (startingPrice <= 0) {
            submission.reset();
            showAlert(Alert.AlertType.WARNING, "Giá không hợp lệ", "Giá khởi điểm phải lớn hơn 0.");
            return;
        }

        LocalDateTime startTime = MyProductsHelper.parseDateTime(editRequest.startDate(), editRequest.startTimeText());
        LocalDateTime endTime = MyProductsHelper.parseDateTime(editRequest.endDate(), editRequest.endTimeText());
        if (startTime == null || endTime == null) {
            submission.reset();
            showAlert(Alert.AlertType.WARNING, "Thời gian không hợp lệ", "Vui lòng nhập ngày và giờ theo định dạng HH:mm.");
            return;
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            submission.reset();
            showAlert(Alert.AlertType.WARNING, "Thời gian không hợp lệ", "Thời gian bắt đầu không được ở quá khứ.");
            return;
        }
        if (!endTime.isAfter(startTime)) {
            submission.reset();
            showAlert(Alert.AlertType.WARNING, "Thời gian không hợp lệ", "Thời gian kết thúc phải lớn hơn thời gian bắt đầu.");
            return;
        }

        if (editRequest.imageFile() != null) {
            CloudStorageUtil.uploadProductImageAsync(editRequest.imageFile())
                    .thenAccept(uploadedImage -> Platform.runLater(() -> {
                        if (uploadedImage == null || uploadedImage.getImageUrl() == null) {
                            submission.reset();
                            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể tải ảnh lên đám mây!");
                            return;
                        }
                        sendProductUpdateRequest(
                                itemObj,
                                trimmedName,
                                editRequest.description(),
                                startingPrice,
                                editRequest.category(),
                                uploadedImage.getImageUrl(),
                                uploadedImage.getThumbnailUrl(),
                                startTime,
                                endTime,
                                submission);
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            submission.reset();
                            showAlert(
                                    Alert.AlertType.ERROR,
                                    "Lỗi kết nối",
                                    "Không thể tải ảnh lên đám mây!");
                        });
                        return null;
                    });
            return;
        }

        sendProductUpdateRequest(
                itemObj,
                trimmedName,
                editRequest.description(),
                startingPrice,
                editRequest.category(),
                editRequest.imageUrl(),
                editRequest.imageThumbUrl(),
                startTime,
                endTime,
                submission);
    }

    private void sendProductUpdateRequest(
            JsonObject itemObj,
            String name,
            String description,
            long startingPrice,
            String category,
            String imageUrl,
            String imageThumbUrl,
            LocalDateTime startTime,
            LocalDateTime endTime,
            MyProductEditDialog.Submission submission) {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.UPDATE_PRODUCT);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());
        request.addProperty("itemId", MyProductsHelper.getInt(itemObj, "itemId", MyProductsHelper.getInt(itemObj, "id", -1)));
        request.addProperty("name", name);
        request.addProperty("description", description == null ? "" : description.trim());
        request.addProperty("startingPrice", startingPrice);
        request.addProperty("category", category == null ? "OTHER" : category);
        request.addProperty("imageUrl", imageUrl == null ? "" : imageUrl.trim());
        request.addProperty("imageThumbUrl", imageThumbUrl == null ? "" : imageThumbUrl.trim());
        request.addProperty("startTime", startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        request.addProperty("endTime", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        ClientSocket.getInstance().sendJsonRequest(request, ActionType.UPDATE_PRODUCT, response ->
                Platform.runLater(() -> handleProductUpdateResponse(response, submission)));
    }

    private void handleProductUpdateResponse(JsonObject response, MyProductEditDialog.Submission submission) {
        boolean success = response.has("success") && response.get("success").getAsBoolean();
        if (success) {
            applyReturnedImageUpdate(response);
            submission.close();
            loadMyPostedProducts();
            return;
        }

        submission.reset();
        showAlert(Alert.AlertType.WARNING, "Không thể thực hiện", MyProductsHelper.getString(response, "message", "Yêu cầu bị từ chối."));
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
            applyReturnedImageUpdate(response);
            showAlert(Alert.AlertType.INFORMATION, "Thành công", successMessage);
            loadMyPostedProducts();
            return;
        }

        showAlert(Alert.AlertType.WARNING, "Không thể thực hiện", MyProductsHelper.getString(response, "message", "Yêu cầu bị từ chối."));
    }

    private void applyReturnedImageUpdate(JsonObject response) {
        if (!response.has("itemId") || !response.has("imageUrl")) {
            return;
        }
        int itemId = MyProductsHelper.getInt(response, "itemId", -1);
        if (itemId <= 0) {
            return;
        }
        for (JsonElement element : loadedProducts) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject product = element.getAsJsonObject();
            int productId = MyProductsHelper.getInt(product, "itemId", MyProductsHelper.getInt(product, "id", -1));
            if (productId != itemId) {
                continue;
            }
            product.addProperty("imageUrl", MyProductsHelper.getString(response, "imageUrl", ""));
            product.addProperty("imageThumbUrl", MyProductsHelper.getString(
                    response,
                    "imageThumbUrl",
                    MyProductsHelper.getString(response, "imageUrl", "")));
            applyFiltersAndRender();
            return;
        }
    }


    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}
