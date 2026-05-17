package com.auction.client.controller.components;

import com.auction.client.controller.MainController;
import com.auction.client.controller.bidder.MainDashboardController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.util.List;

public class SidebarController {

    @FXML private Button btnDashboard, btnAuctions, btnMyAuctions, btnMyProducts, btnFollowed, btnPostAuction;

    private List<Button> allButtons;

    @FXML
    public void initialize() {
        // Gom các nút vào list để quản lý class cho nhàn
        allButtons = List.of(btnDashboard, btnAuctions, btnMyAuctions, btnMyProducts, btnFollowed);
    }

    // Hàm lõi: Chuyển class "nav-button-active" sang nút được bấm
    private void setButtonActive(Button activeBtn) {
        // 1. Duyệt qua tất cả các nút
        for (Button btn : allButtons) {
            if (btn != null) {
                // Xóa sạch cả 2 class để đưa về trạng thái "trắng"
                btn.getStyleClass().removeAll("nav-button", "nav-button-active");

                // Nếu nút này là nút đang được bấm
                if (btn == activeBtn) {
                    btn.getStyleClass().add("nav-button-active"); // Gán màu đỏ
                } else {
                    btn.getStyleClass().add("nav-button"); // Gán màu bình thường
                }
            }
        }
    }

    // HÀM LỌC DANH MỤC (DÙNG CHUNG)
    @FXML
    private void handleFilterCategory(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        String category = clickedBtn.getText().trim();

        // 1. Highlight nút bấm (m làm rồi)
        setButtonActive(clickedBtn);

        // 2. Ép MainController quay về trang Dashboard (nếu user đang ở trang khác)
        MainController.instance.setCenterContent("/fxml/bidder/MainDashboard.fxml");

        // 3. Lấy cái Controller vừa được load lên
        Object currentCtrl = MainController.instance.getCurrentCenterController();

        // 4. Nếu đúng là trang Dashboard thì ra lệnh lọc
        if (currentCtrl instanceof MainDashboardController) {
            ((MainDashboardController) currentCtrl).onCategorySelected(category);
        }
    }

    @FXML
    private void handleOpenDashboard(ActionEvent event) {
        setButtonActive(btnDashboard);
        MainController.instance.setCenterContent("/fxml/bidder/MainDashboard.fxml");
    }

    @FXML
    private void handleOpenAuctions(ActionEvent event) {
        setButtonActive(btnAuctions);
        MainController.instance.setCenterContent("/fxml/bidder/AuctionListScreen.fxml");
    }

    @FXML
    private void handleOpenMyAuctions(ActionEvent event) {
        setButtonActive(btnMyAuctions);
        MainController.instance.setCenterContent("/fxml/bidder/MyAuctions.fxml");
    }

    @FXML
    private void handleOpenMyProducts(ActionEvent event) {
        setButtonActive(btnMyProducts);
        MainController.instance.setCenterContent("/fxml/seller/MyProducts.fxml");
    }

    @FXML
    private void handleOpenFollowed(ActionEvent event) {
        setButtonActive(btnFollowed);
        MainController.instance.setCenterContent("/fxml/bidder/FollowedAuctions.fxml");
    }

    @FXML
    private void handleOpenPostAuction(ActionEvent event) {
        // Nút đăng bài thường không cần giữ trạng thái active vì nó mở Form
        MainController.instance.setCenterContent("/fxml/seller/SellerDashboard.fxml");
    }

    // =========================================================================
    // LOGIC ĐĂNG XUẤT
    // =========================================================================
    @FXML
    private void handleLogout(ActionEvent event) {
        org.slf4j.LoggerFactory.getLogger(getClass()).info("Người dùng bấm nút Đăng xuất hệ thống.");

        try {
            // 1. Tải file giao diện Đăng nhập gốc
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/auth/Login.fxml"));
            javafx.scene.Parent loginRoot = loader.load();

            // 2. Lấy Stage (cửa sổ Windows) hiện tại từ nút bấm
            javafx.stage.Stage currentStage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

            // 3. Ép cái Stage phải ở trạng thái Maximized TRƯỚC để lấy đúng kích thước màn hình thật
            currentStage.setMaximized(true);
            currentStage.show();

            // 4. Lấy ra kích thước chiều rộng và chiều cao thực tế của màn hình laptop đang hiển thị
            double actualWidth = currentStage.getWidth();
            double actualHeight = currentStage.getHeight();

            // 5. Tạo một hộp StackPane động bằng code Java để bao bọc, ép màu nền khít toàn màn hình
            javafx.scene.layout.StackPane masterRoot = new javafx.scene.layout.StackPane();
            masterRoot.setStyle("-fx-background-color: #FDFBF9;"); // Màu nền chủ đạo của app
            masterRoot.setPrefWidth(actualWidth);
            masterRoot.setPrefHeight(actualHeight);

            // Nhét form Login vào và ép nó CĂN GIỮA TUYỆT ĐỐI dựa trên kích thước thật vừa lấy
            masterRoot.getChildren().add(loginRoot);
            masterRoot.setAlignment(loginRoot, javafx.geometry.Pos.CENTER);

            // 6. Tạo Scene mới từ cái hộp masterRoot đã căn giữa xịn xò này
            javafx.scene.Scene loginScene = new javafx.scene.Scene(masterRoot, actualWidth, actualHeight);
            currentStage.setScene(loginScene);

        } catch (java.io.IOException e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Lỗi khi chuyển hướng giao diện đăng xuất: {}", e.getMessage());
        }
    }
}