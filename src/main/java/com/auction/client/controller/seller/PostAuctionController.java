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

    // --- CÁC BIẾN GIAO DIỆN (Đã sửa khớp 100% với FXML ) ---
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

    @FXML
    public void initialize() {
        if (cbCategory != null) {
            cbCategory.getItems().addAll("Điện tử", "Nghệ thuật", "Xe cộ", "Đồ sưu tầm", "Trang sức");
        }
        logger.info("Seller Dashboard initialized - Sẵn sàng nhận thông tin đấu giá.");
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
            double startPrice = Double.parseDouble(txtStartingPrice.getText());
            double increment = Double.parseDouble(txtIncrement.getText());

            // Xử lý thời gian (Ghép Date và Time)
            LocalDateTime start = LocalDateTime.of(dpStartDate.getValue(), LocalTime.parse(txtStartTime.getText()));
            LocalDateTime end = LocalDateTime.of(dpEndDate.getValue(), LocalTime.parse(txtEndTime.getText()));

            // 3. Đóng gói DTO (Dựa trên BaseDTOs )
            // BaseDTOs.Request request = new BaseDTOs.Request("CREATE_AUCTION");
            // request.setPayload(new AuctionItem(name, startPrice, increment, start, end));

            // 4. Gửi qua NetworkManager
            // NetworkManager.getInstance().sendRequest(request);

            logger.info("Đã gửi yêu cầu tạo đấu giá cho sản phẩm: {}", name);
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Sản phẩm của m đã được gửi lên hệ thống!");

        } catch (Exception e) {
            logger.error("Lỗi khi parse dữ liệu form: {}", e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Kiểm tra lại giá tiền hoặc định dạng giờ (HH:mm) nhé m!");
        }
    }

    private boolean isInputInvalid() {
        if (txtProductName.getText().isEmpty() || txtStartingPrice.getText().isEmpty() || dpStartDate.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Mấy ô có dấu * là bắt buộc phải điền đấy m ơi!");
            return true;
        }
        return false;
    }

    @FXML
    public void goToHome(ActionEvent event) {
        logger.info("Chuyển về trang chủ.");
        // Logic chuyển Scene
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}