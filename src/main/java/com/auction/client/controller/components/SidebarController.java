package com.auction.client.controller.components;

import com.auction.client.controller.MainController;
import com.auction.client.controller.auth.UserSession;
import com.auction.client.networkclient.ClientSocket;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import java.util.List;


public class SidebarController {

    // Biến static công khai để LoginController có thể bốc và điều khiển từ xa!
    public static SidebarController instance;
    private static int unreadNotifications;
    @FXML private Button btnDashboard, btnAuctions, btnMyAuctions, btnMyProducts, btnFollowed, btnPostAuction, btnChat;
    @FXML private Label lblNotificationBadge;
    // Khai báo thêm biến nút Ví tiền kết nối với file FXML
    @FXML private Button btnWallet;

    private List<Button> allButtons;

    @FXML
    public void initialize() {
        // GÁN INSTANCE: Ghi nhận chính bộ điều khiển này ngay khi thanh Sidebar được nạp lên giao diện
        instance = this;
        allButtons = List.of(btnDashboard, btnAuctions, btnMyAuctions, btnMyProducts, btnFollowed, btnWallet, btnPostAuction, btnChat);

        // Kích nổ hàm áp dụng phân quyền ngay khi nạp giao diện ban đầu
        applyRolePermissions();
        updateNotificationBadge();
        dongBoThongBaoChuaDocTuServer();
    }

    /**
     * HÀM PHÂN QUYỀN CÔNG KHAI (PUBLIC):
     * LoginController hoặc bất kỳ đâu đều có thể gọi qua: SidebarController.instance.applyRolePermissions();
     */
    public void applyRolePermissions() {
        Platform.runLater(() -> {
            try {
                // Lấy vai trò chuẩn ENUM viết hoa từ UserSession
                String currentRole = UserSession.getCurrentRole();
                org.slf4j.LoggerFactory.getLogger(getClass()).info("[Sidebar] Đang tiến hành áp dụng phân quyền cho vai trò: {}", currentRole);

                if (currentRole != null && "SELLER".equalsIgnoreCase(currentRole)) {
                    org.slf4j.LoggerFactory.getLogger(getClass()).info("[Sidebar] Vai trò: SELLER - Tiến hành thu gọn menu Bidder.");

                    // Ẩn sạch các tính năng không liên quan đến người bán (setManaged giúp dồn dòng menu khít rịt)
                    if (btnDashboard != null) { btnDashboard.setVisible(false); btnDashboard.setManaged(false); }
                    if (btnAuctions != null) { btnAuctions.setVisible(false); btnAuctions.setManaged(false); }
                    if (btnMyAuctions != null) { btnMyAuctions.setVisible(false); btnMyAuctions.setManaged(false); }
                    if (btnFollowed != null) { btnFollowed.setVisible(false); btnFollowed.setManaged(false); }

                    // Hiện các tính năng của Seller chính hiệu
                    if (btnPostAuction != null) { btnPostAuction.setVisible(true); btnPostAuction.setManaged(true); }
                    if (btnMyProducts != null) { btnMyProducts.setVisible(true); btnMyProducts.setManaged(true); }

                    // Ép ruột mặc định bên phải nhảy thẳng vào trang quản lý sản phẩm của người bán
                    MainController.instance.setCenterContent("/fxml/seller/MyProducts.fxml");
                    setButtonActive(btnMyProducts);
                } else {
                    org.slf4j.LoggerFactory.getLogger(getClass()).info("[Sidebar] Vai trò: BIDDER hoặc Chưa Đăng nhập - Tiến hành ẩn nút của Seller.");

                    // Người đi mua hoặc chưa đăng nhập thì không được quyền nhìn thấy nút đăng sản phẩm và nút quản lý đồ bán
                    if (btnPostAuction != null) { btnPostAuction.setVisible(false); btnPostAuction.setManaged(false); }
                    if (btnMyProducts != null) { btnMyProducts.setVisible(false); btnMyProducts.setManaged(false); }

                    // Hiện đầy đủ các tính năng của Bidder
                    if (btnDashboard != null) { btnDashboard.setVisible(true); btnDashboard.setManaged(true); }
                    if (btnAuctions != null) { btnAuctions.setVisible(true); btnAuctions.setManaged(true); }
                    if (btnMyAuctions != null) { btnMyAuctions.setVisible(true); btnMyAuctions.setManaged(true); }
                    if (btnFollowed != null) { btnFollowed.setVisible(true); btnFollowed.setManaged(true); }
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Chưa lấy được vai trò người dùng (Có thể do chạy dev thô): " + e.getMessage());
            }
        });
    }

    // Hàm lõi: Chuyển class "nav-button-active" sang nút được bấm
    private void setButtonActive(Button activeBtn) {
        if (allButtons == null) return;
        // Duyệt qua tất cả các nút
        for (Button btn : allButtons) {
            if (btn != null) {
                // Xóa sạch cả 2 class để đưa về trạng thái "trắng"
                btn.getStyleClass().removeAll("nav-button", "nav-button-active");

                // Nếu nút này là nút đang được bấm
                if (btn == activeBtn) {
                    btn.getStyleClass().add("nav-button-active"); // Gán màu active
                } else {
                    btn.getStyleClass().add("nav-button"); // Gán màu bình thường
                }
            }
        }
    }

    // HÀM LỌC DANH MỤC
    @FXML
    private void handleFilterCategory(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        // 1. Đổi màu nút đang được bấm
        setButtonActive(clickedBtn);

        String buttonText = clickedBtn.getText().trim();
        String category = mapCategory(buttonText);

        // 2. Ép hệ thống chuyển hướng sang màn hình "Tất cả phiên"
        boolean alreadyShowingAuctionList = MainController.instance.getCurrentCenterController()
                instanceof com.auction.client.controller.bidder.AuctionListScreenController;
        MainController.instance.setCenterContent(
                "/fxml/bidder/AuctionListScreen.fxml",
                !alreadyShowingAuctionList
        );

        // 3. Lấy ra Controller của màn hình vừa được load lên
        Object currentCtrl = MainController.instance.getCurrentCenterController();

        // 4. Kiểm tra Đa hình: Truyền lệnh lọc qua Interface
        if (currentCtrl instanceof com.auction.client.interfaces.CategoryFilterListener) {
            ((com.auction.client.interfaces.CategoryFilterListener) currentCtrl).onCategorySelected(category);
        }
    }

    private String mapCategory(String buttonText) {
        if ("Điện tử".equals(buttonText)) return "ELECTRONICS";
        if ("Xe cộ".equals(buttonText)) return "VEHICLE";
        if ("Nghệ thuật".equals(buttonText)) return "ART";
        if ("Khác".equals(buttonText)) return "OTHER";
        return "ALL";
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
        setButtonActive(btnPostAuction);
        MainController.instance.setCenterContent("/fxml/seller/SellerDashboard.fxml");
    }

    @FXML
    private void handleOpenWallet(ActionEvent event) {
        setButtonActive((Button) event.getSource());
        MainController.instance.setCenterContent("/fxml/components/Wallet.fxml");
    }

    @FXML
    private void handleOpenChat(ActionEvent event) {
        setButtonActive(btnChat); // Đã đồng bộ sang nút biến vừa khai báo
        MainController.instance.setCenterContent("/fxml/components/Chat.fxml");
        clearUnreadNotifications();
    }

    public static void recordUnreadNotification() {
        unreadNotifications++;
        if (instance != null) {
            Platform.runLater(instance::updateNotificationBadge);
        }
    }

    public static void clearUnreadNotifications() {
        unreadNotifications = 0;
        if (instance != null) {
            Platform.runLater(instance::updateNotificationBadge);
        }
    }

    private void updateNotificationBadge() {
        if (lblNotificationBadge == null) {
            return;
        }

        boolean hasUnread = unreadNotifications > 0;
        lblNotificationBadge.setManaged(hasUnread);
        lblNotificationBadge.setVisible(hasUnread);
        lblNotificationBadge.setText(unreadNotifications > 99 ? "99+" : String.valueOf(unreadNotifications));
    }

    private void dongBoThongBaoChuaDocTuServer() {
        if (UserSession.getUserId() <= 0) {
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_SYSTEM_NOTIFICATIONS);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(request, "SYSTEM_NOTIFICATIONS_RESPONSE", response -> {
            if (!response.has("success") || !response.get("success").getAsBoolean()
                    || !response.has("unreadCount")) {
                return;
            }
            unreadNotifications = Math.max(response.get("unreadCount").getAsInt(), 0);
            Platform.runLater(this::updateNotificationBadge);
        });
    }

    // =========================================================================
    // LOGIC ĐĂNG XUẤT (LOGOUT) CHUẨN KIẾN TRÚC MỚI
    // =========================================================================
    @FXML
    private void handleLogout(ActionEvent event) {
        org.slf4j.LoggerFactory.getLogger(getClass()).info("Người dùng bấm nút Đăng xuất hệ thống.");

        //  Xóa sạch dấu vết Session cũ để bảo mật, tránh xung đột quyền tài khoản sau!
        UserSession.clear();
        clearUnreadNotifications();
        ChatController.instance = null;
        com.auction.client.networkclient.PushHandler.clearNotifications();
        com.auction.client.util.ViewCacheManager.clear();

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
