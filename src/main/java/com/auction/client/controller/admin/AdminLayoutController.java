package com.auction.client.controller.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

// import com.auction.client.util.NavigationUtils;

// Đây là controller cho AdminDashboard.fxml, quản lý giao diện chính của admin sau khi đăng nhập
// Các chức năng như quản lý phiên đấu giá, sản phẩm, người dùng sẽ được nạp vào contentPane khi nhấn vào các nút ở sidebar
// Tạm thời để trống các hàm xử lý điều hướng và nạp view.
public class AdminLayoutController implements Initializable {

    // --- Sidebar Buttons ---
    @FXML private Button btnDashboard;
    @FXML private Button btnAuctions;
    @FXML private Button btnProducts;
    @FXML private Button btnBidders;
    @FXML private Button btnSellers;
    @FXML private Button btnTransactions;
    @FXML private Button btnInvoices;
    @FXML private Button logOut; // Nút đăng xuất


    // --- Header Elements ---
    @FXML private Label lblPageTitle;
    @FXML private Label lblBreadcrumb;
    @FXML private Label lblAdminName;

    // --- Layout Containers ---
    @FXML private StackPane contentPane;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Mặc định load trang Dashboard khi vừa vào
        loadView("/fxml/admin/DashboardView.fxml", "Dashboard");
        lblAdminName.setText("Super Admin"); // Có thể lấy từ UserSession
    }

    /**
     * Xử lý điều hướng khi nhấn vào các nút ở Sidebar
     */
    @FXML
    private void navigate(ActionEvent event) {
        Button sourceBtn = (Button) event.getSource();
        System.out.println("đang nhấn: " + sourceBtn.getText());
        String btnId = sourceBtn.getId();

        // Reset tất cả style các nút về bình thường
        resetNavStyles();
        // Thêm class active cho nút vừa nhấn
        sourceBtn.getStyleClass().add("admin-nav-item-active");

        switch (btnId) {
            case "btnDashboard":
                loadView("/fxml/admin/DashboardView.fxml", "Dashboard");
                break;
            case "btnAuctions":
                loadView("/fxml/admin/AuctionsView.fxml", "Phiên đấu giá");
                break;
            case "btnProducts":
                loadView("/fxml/admin/ProductsCensorView.fxml", "Duyệt sản phẩm");
                break;
            case "btnBidders":
                loadView("/fxml/admin/BiddersView.fxml", "Quản lý Bidder");
                break;
            case "btnSellers":
                loadView("/fxml/admin/SellersView.fxml", "Quản lý Seller");
                break;
            case "btnTransactions":
                loadView("/fxml/admin/TransactionsView.fxml", "Giao dịch");
                break;
            case "btnInvoices":
                loadView("/fxml/admin/InvoicesView.fxml", "Hóa đơn");
                break;
        }
    }

    /**
     * Nạp file FXML con vào vùng contentPane
     */
    private void loadView(String fxmlPath, String title) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(view);

            // Cập nhật tiêu đề trang và Breadcrumb
            lblPageTitle.setText(title);
            lblBreadcrumb.setText(title);

        } catch (IOException e) {
            System.err.println("Không thể load view: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Xóa trạng thái Active của các nút điều hướng
     */
    private void resetNavStyles() {
        btnDashboard.getStyleClass().remove("admin-nav-item-active");
        btnAuctions.getStyleClass().remove("admin-nav-item-active");
        btnProducts.getStyleClass().remove("admin-nav-item-active");
        btnBidders.getStyleClass().remove("admin-nav-item-active");
        btnSellers.getStyleClass().remove("admin-nav-item-active");
        btnTransactions.getStyleClass().remove("admin-nav-item-active");
        btnInvoices.getStyleClass().remove("admin-nav-item-active");
    }

    /**
     * Thu gọn/Mở rộng Sidebar (Toggle)
     */
    // @FXML
    // private void toggleSidebar() {
    //     // Logic thu gọn sidebar: Bạn có thể chỉnh prefWidth hoặc ẩn/hiện
    //     Node leftNode = ((BorderPane) contentPane.getScene().getRoot()).getLeft();
    //     if (leftNode.isVisible()) {
    //         leftNode.setVisible(false);
    //         leftNode.setManaged(false);
    //     } else {
    //         leftNode.setVisible(true);
    //         leftNode.setManaged(true);
    //     }
    // }

    /**
     * Xử lý tìm kiếm toàn cầu
     */

    /**
     * Xử lý Đăng xuất
     */
    @FXML
    private void handleLogout() {
        // Hiển thị Alert xác nhận
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận đăng xuất");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất không?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            // Chuyển về màn hình Login
            System.out.println("Đang đăng xuất...");
            try {
                Stage stage = (Stage) logOut.getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auth/Login.fxml"));
                Parent root = loader.load();
                stage.getScene().setRoot(root);
                stage.setTitle("Đăng nhập hệ thống");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Không thể mở màn hình đăng nhập!");
            }
        }
    }
}
