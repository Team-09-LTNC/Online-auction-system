package com.auction.client.controller.seller;

import com.auction.client.network.NetworkManager;
import com.auction.common.dto.BaseDTOs;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;

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

    @FXML
    public void initialize() {
        if (cbCategory != null) {
            cbCategory.getItems().clear();
            cbCategory.getItems().addAll("Điện tử", "Xe cộ","Nghệ thuật", "Đồ sưu tầm", "Khác");
        }

        // --- LOGIC LIVE PREVIEW (Gõ bên trái, nhảy chữ bên phải) ---
        setupLivePreview();

        logger.info("Seller Dashboard initialized - Sẵn sàng nhận thông tin đấu giá.");
    }

    private void setupLivePreview() {
        // 1. Cập nhật tên sản phẩm
        if (txtProductName != null && lblPreviewName != null) {
            txtProductName.textProperty().addListener((obs, oldVal, newVal) -> {
                lblPreviewName.setText(newVal.isEmpty() ? "Tên sản phẩm mẫu" : newVal);
            });
        }

        // 2. Cập nhật giá (Thêm format dấu phẩy ngăn cách hàng nghìn)
        if (txtStartingPrice != null && lblPreviewPrice != null) {
            txtStartingPrice.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.isEmpty()) {
                    lblPreviewPrice.setText("0 đ");
                } else {
                    try {
                        // Loại bỏ ký tự không phải số nếu user lỡ gõ chữ
                        String cleanString = newVal.replaceAll("[^\\d]", "");
                        double price = Double.parseDouble(cleanString);
                        // Format kiểu: 1,000,000 đ
                        lblPreviewPrice.setText(String.format("%,.0f đ", price));
                    } catch (NumberFormatException e) {
                        lblPreviewPrice.setText("Giá không hợp lệ");
                    }
                }
            });
        }

        // 3. Cập nhật danh mục (Dùng try-catch hoặc check Null vì nãy mình bỏ cái nhãn này trong Preview rồi)
        if (cbCategory != null) {
            cbCategory.valueProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("User chọn danh mục: " + newVal);

            });
        }
    }

    // --- HÀM XỬ LÝ KHI BẤM NÚT "TIẾP TỤC" (onAction="#handleSubmitAuction") ---
    @FXML
    public void handleSubmitAuction(ActionEvent event) {
        logger.debug("Người dùng bấm nút Đăng sản phẩm.");

        // 1. Validation (Kiểm tra dữ liệu)
        if (isInputInvalid()) {
            return;
        }

        try {
            // 2. Thu thập dữ liệu
            String name = txtProductName.getText();
            double startPrice = Double.parseDouble(txtStartingPrice.getText().replace(",", ""));
            double increment = Double.parseDouble(txtIncrement.getText().replace(",", ""));
            String category = cbCategory.getValue();
            boolean antiSniping = chkAntiSniping.isSelected();

            // Xử lý thời gian (Ghép Date và Time)
            LocalDateTime start = LocalDateTime.of(dpStartDate.getValue(), LocalTime.parse(txtStartTime.getText()));
            LocalDateTime end = LocalDateTime.of(dpEndDate.getValue(), LocalTime.parse(txtEndTime.getText()));

            // 3. Đóng gói DTO (Ráp nối với logic Server )
            BaseDTOs.CreateAuctionRequest request = new BaseDTOs.CreateAuctionRequest();

            // requestId có thể dùng UUID hoặc để trống nếu server không bắt buộc
            request.requestId = java.util.UUID.randomUUID().toString();

            request.productName = name;
            request.category = category;
            request.description = txtDescription.getText();
            request.startPrice = startPrice;
            request.increment = increment;
            request.startTime = start.toString();
            request.endTime = end.toString();
            request.antiSniping = antiSniping;

            // 4. Gửi qua NetworkManager (Mở comment khi đã thông Socket với Server)
            try {
                // Chuyển đối tượng request thành JSON và gửi đi
                NetworkManager.getInstance().sendRequest(request);

                logger.info("Đã bắn gói tin CREATE_AUCTION lên Server.");

                // Đừng hiện Alert thành công vội, vì mới chỉ là "gửi đi" thôi
                // Chờ Server trả lời SUCCESS thì mới báo thành công
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không gửi được bài, kiểm tra lại Server!");
            }

            logger.info("Đã gửi yêu cầu tạo đấu giá cho sản phẩm: {}", name);
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Sản phẩm của bạn đã được gửi lên hệ thống!");

        } catch (Exception e) {
            logger.error("Lỗi khi parse dữ liệu form: {}", e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Kiểm tra lại giá tiền (chỉ nhập số) hoặc định dạng giờ (HH:mm) nhé !");
        }
    }

    private boolean isInputInvalid() {
        if (txtProductName.getText().isEmpty() || txtStartingPrice.getText().isEmpty() || dpStartDate.getValue() == null || txtStartTime.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Mấy ô dấu * là bắt buộc phải điền!");
            return true;
        }
        return false;
    }

    @FXML
    public void goToHome(ActionEvent event) {
        logger.info("Chuyển về trang chủ.");
        // Logic điều hướng quay lại trang chủ dùng MainController
        // com.auction.client.controller.MainController.instance.setCenterContent("/fxml/MainDashboard.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}