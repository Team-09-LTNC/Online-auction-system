package com.auction.client.controller.seller;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.dto.ItemDTOs;
import com.auction.common.enums.ActionType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostAuctionController {

    // 1. Thêm Logger thay cho System.out.println
    private static final Logger logger = LoggerFactory.getLogger(PostAuctionController.class);

    // --- CÁC BIẾN GIAO DIỆN  ---
    @FXML private TextField txtProductName;
    @FXML private ComboBox<String> cbCategory;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtStartingPrice;
    @FXML private TextField txtIncrement;
    @FXML private TextField txtBuyNowPrice;
    @FXML private DatePicker dpStartDate;
    @FXML private TextField txtStartTime;
    @FXML private DatePicker dpEndDate;
    @FXML private TextField txtEndTime;
    @FXML private CheckBox chkAntiSniping;

    // --- CÁC BIẾN PREVIEW (Thêm vào để làm tính năng Live Preview) ---
    @FXML private Label lblPreviewName;
    @FXML private Label lblPreviewPrice;
    @FXML private Label lblPreviewCategory;

    // 🔥 KHÔI PHỤC: Thêm các biến quản lý ảnh Preview để kết nối với FXML mới
    @FXML private ImageView imgPreview;
    @FXML private Button btnUploadImage;

    @FXML
    public void initialize() {
        if (cbCategory != null) {
            cbCategory.getItems().clear();
            cbCategory.getItems().addAll("Điện tử", "Xe cộ","Nghệ thuật", "Đồ sưu tầm", "Khác");
        }
        setupLivePreview();
        logger.info("Seller Dashboard initialized - Sẵn sàng nhận thông tin đấu giá.");
    }

    private void setupLivePreview() {
        if (txtProductName != null && lblPreviewName != null) {
            txtProductName.textProperty().addListener((obs, oldVal, newVal) -> {
                lblPreviewName.setText(newVal.isEmpty() ? "Tên sản phẩm mẫu" : newVal);
            });
        }
        if (txtStartingPrice != null && lblPreviewPrice != null) {
            txtStartingPrice.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.isEmpty()) {
                    lblPreviewPrice.setText("0 đ");
                } else {
                    try {
                        String cleanString = newVal.replaceAll("[^\\d]", "");
                        double price = Double.parseDouble(cleanString);
                        lblPreviewPrice.setText(String.format("%,.0f đ", price));
                    } catch (NumberFormatException e) {
                        lblPreviewPrice.setText("Giá không hợp lệ");
                    }
                }
            });
        }
        if (cbCategory != null) {
            cbCategory.valueProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("User chọn danh mục: " + newVal);
            });
        }
    }

    // --- HÀM XỬ LÝ KHI BẤM NÚT "TIẾP TỤC" ---
    @FXML
    public void handleSubmitAuction(ActionEvent event) {
        logger.debug("Người dùng bấm nút Đăng sản phẩm.");

        if (isInputInvalid()) return;

        try {
            // 2. Thu thập dữ liệu
            String name = txtProductName.getText();
            long startPrice = Long.parseLong(txtStartingPrice.getText().replace(",", ""));
            String category = cbCategory.getValue();
            String description = txtDescription.getText() != null ? txtDescription.getText() : "";

            // 3. Đóng gói DTO - SỬ DỤNG CHUẨN DTO MÀ SERVER YÊU CẦU
            // Truyền 0 cho sellerId vì Server (ProductController) sẽ tự trích xuất qua ClientSession
            ItemDTOs.CreateItemRequest requestDto = new ItemDTOs.CreateItemRequest(name, description, startPrice, category, 0);

            // Ép JSON type thành CREATE_PRODUCT để khớp chính xác Router của Server
            JsonObject reqJson = new Gson().toJsonTree(requestDto).getAsJsonObject();
            reqJson.addProperty("type", ActionType.CREATE_PRODUCT);

            // 🔥 KHÔI PHỤC: Đóng gói thêm trường đường dẫn hình ảnh vào JsonObject trước khi gửi đi
            reqJson.addProperty("imageUrl", this.selectedImagePath);

            // 4. Gửi qua NetworkManager và ĐỢI KẾT QUẢ
            ClientSocket.getInstance().sendJsonRequest(reqJson, "CREATE_ITEM_RESPONSE", response -> {
                Platform.runLater(() -> {
                    boolean success = response.has("success") && response.get("success").getAsBoolean();
                    if (success) {
                        logger.info("Đã gửi yêu cầu tạo đấu giá cho sản phẩm: {}", name);
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Sản phẩm của bạn đã được đăng chờ hệ thống duyệt!");
                        goToHome(event);
                    } else {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi không xác định!";
                        showAlert(Alert.AlertType.ERROR, "Thất bại", msg);
                    }
                });
            });

        } catch (Exception e) {
            logger.error("Lỗi khi parse dữ liệu form: {}", e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Kiểm tra lại giá tiền (chỉ nhập số) nhé!");
        }
    }

    private boolean isInputInvalid() {
        if (txtProductName.getText() == null || txtProductName.getText().trim().isEmpty() ||
                txtStartingPrice.getText() == null || txtStartingPrice.getText().trim().isEmpty() ||
                cbCategory.getValue() == null) {

            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng điền đầy đủ các thông tin bắt buộc!");
            return true;
        }
        return false;
    }

    @FXML
    public void goToHome(ActionEvent event) {
        logger.info("Chuyển về trang chủ.");
        // Logic điều hướng quay lại trang chủ dùng MainController
        // com.auction.client.controller.MainController.instance.setCenterContent("/fxml/seller/MyProducts.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // =========================================================================
    //LOGIC CHỌN ẢNH HỆ THỐNG VÀ XỬ LÝ PREVIEW ĐÊM QUA
    // =========================================================================
    private String selectedImagePath = null;

    @FXML
    public void handleUploadImage(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm đấu giá");

        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        java.io.File selectedFile = fileChooser.showOpenDialog(((javafx.scene.Node) event.getSource()).getScene().getWindow());

        if (selectedFile != null) {
            this.selectedImagePath = selectedFile.toURI().toString();

            javafx.scene.image.Image image = new javafx.scene.image.Image(this.selectedImagePath);
            if (imgPreview != null) {
                imgPreview.setImage(image);
            }
            logger.info("[FileChooser] Đã chọn ảnh thành công: {}", this.selectedImagePath);
        }
    }
}