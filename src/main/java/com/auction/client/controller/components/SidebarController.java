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
        com.auction.client.controller.MainController.instance.setCenterContent("/fxml/bidder/MainDashboard.fxml");

        // 3. Lấy cái Controller vừa được load lên
        Object currentCtrl = com.auction.client.controller.MainController.instance.getCurrentCenterController();

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
}