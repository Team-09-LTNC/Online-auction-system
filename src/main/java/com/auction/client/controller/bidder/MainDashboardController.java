package com.auction.client.controller.bidder;

import com.auction.client.controller.components.ProductCardController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainDashboardController implements Initializable, com.auction.client.interfaces.CategoryFilterListener {

    private static final Logger logger = LoggerFactory.getLogger(MainDashboardController.class);

    @FXML private FlowPane productFlowPane;

    // 🔥 BIẾN UI: Tên và lời chào dynamic
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private Label lblBannerWelcome;

    // 🔥 BIẾN UI BỔ SUNG: 4 ô số liệu thống kê động
    @FXML private Label lblActiveAuctions;
    @FXML private Label lblEndingSoonAuctions;
    @FXML private Label lblFollowedAuctions;
    @FXML private Label lblMyBidsCount;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Bidder đã vào Dashboard chính - Đang nạp danh sách sản phẩm.");

        // Xóa sạch các card cũ (nếu có) trước khi nạp mới
        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadMockProducts();
        }

        // 🔥 CẬP NHẬT TÊN USER ĐĂNG NHẬP
        updateDashboardUserInfo();

        // 🔥 CẬP NHẬT 4 Ô SỐ LIỆU THỐNG KÊ ĐỘNG
        updateStatistics();
    }

    private void updateDashboardUserInfo() {
        try {
            String currentUserName = "Người dùng";
            String currentUserRole = "BIDDER";

            if (lblHeaderName != null) lblHeaderName.setText("Chào, " + currentUserName);
            if (lblBannerWelcome != null) lblBannerWelcome.setText("Chào mừng trở lại, " + currentUserName + "! 👋");

            if (lblHeaderRole != null) {
                if ("BIDDER".equalsIgnoreCase(currentUserRole)) {
                    lblHeaderRole.setText("Bidder");
                } else if ("SELLER".equalsIgnoreCase(currentUserRole)) {
                    lblHeaderRole.setText("Seller");
                } else {
                    lblHeaderRole.setText(currentUserRole);
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi khi load thông tin User lên Header: {}", e.getMessage());
        }
    }

    // =========================================================================
    // 🔥LOGIC ĐỔ SỐ LIỆU THỐNG KÊ ĐỘNG TỪ DATABASE
    // =========================================================================
    private void updateStatistics() {
        try {
            // Tạm thời mock số liệu động (Tối ráp Socket với Kiên, gói tin trả về
            // bốc từ DB lên bao nhiêu thì truyền vào các hàm setText này bấy nhiêu)
            int activeCount = 145;      // Tổng số phiên 'OPEN' dưới DB
            int endingSoonCount = 18;   // Số phiên còn dưới 1 tiếng dưới DB
            int followedCount = 32;     // Số dòng trong bảng follow của userId này
            int myBidsCount = 7;        // Số phiên userId này đã từng tham gia trả giá

            if (lblActiveAuctions != null) lblActiveAuctions.setText(String.valueOf(activeCount));
            if (lblEndingSoonAuctions != null) lblEndingSoonAuctions.setText(String.valueOf(endingSoonCount));
            if (lblFollowedAuctions != null) lblFollowedAuctions.setText(String.valueOf(followedCount));
            if (lblMyBidsCount != null) lblMyBidsCount.setText(String.valueOf(myBidsCount));

            logger.info("Đã đồng bộ thành công số liệu thống kê lên Dashboard.");
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật số liệu thống kê lên UI: {}", e.getMessage());
        }
    }

    private void loadMockProducts() {
        // muốn hiện bao nhiêu sản phẩm cũng được, FlowPane tự xếp
        for (int i = 0; i < 12; i++) {
            try {
                // 2. Nạp file FXML của cái Card sản phẩm
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/fxml/components/ProductCard.fxml"));
                VBox card = loader.load();

                // 3. Lấy Controller để đổ dữ liệu giả
                ProductCardController controller = loader.getController();

                if (i % 2 == 0) {
                    controller.setProductData("Mercedes-Benz S450 " + i, 3500000000.0, "02:15:30", "Đang diễn ra");
                } else {
                    controller.setProductData("iPhone 15 Pro Max " + i, 32000000.0, "00:00:00", "Đã kết thúc");
                }

                productFlowPane.getChildren().add(card);

            } catch (IOException e) {
                logger.error("Không nạp được ProductCard.fxml: {}", e.getMessage());
            }
        }
    }

    @Override
    public void onCategorySelected(String category) {
        System.out.println("LOG: Dashboard đang thực hiện lọc cho danh mục: " + category);

        productFlowPane.getChildren().clear();
        loadMockProducts();
    }
}