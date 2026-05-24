package com.auction.client.controller.admin;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller chinh cho man hinh admin layout.
 */
public class AdminLayoutController implements Initializable {
  private static final Logger logger = LoggerFactory.getLogger(AdminLayoutController.class);

  @FXML private Button btnDashboard;
  @FXML private Button btnAuctions;
  @FXML private Button btnProducts;
  @FXML private Button btnBidders;
  @FXML private Button btnSellers;
  @FXML private Button btnTransactions;
  @FXML private Button btnInvoices;
  @FXML private Button logOut;

  @FXML private Label lblPageTitle;
  @FXML private Label lblBreadcrumb;
  @FXML private Label lblAdminName;
  @FXML private StackPane contentPane;

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    loadView("/fxml/admin/DashboardView.fxml", "Dashboard");
    lblAdminName.setText("Super Admin");
  }

  @FXML
  private void navigate(ActionEvent event) {
    Button sourceBtn = (Button) event.getSource();
    logger.info("Admin nhan nut: {}", sourceBtn.getText());
    String btnId = sourceBtn.getId();

    resetNavStyles();
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
      default:
        logger.warn("Khong tim thay nut dieu huong cho id={}", btnId);
    }
  }

  private void loadView(String fxmlPath, String title) {
    try {
      Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
      contentPane.getChildren().setAll(view);
      lblPageTitle.setText(title);
      lblBreadcrumb.setText(title);
    } catch (IOException e) {
      logger.error("Loi load view {}.", fxmlPath, e);
    }
  }

  private void resetNavStyles() {
    btnDashboard.getStyleClass().remove("admin-nav-item-active");
    btnAuctions.getStyleClass().remove("admin-nav-item-active");
    btnProducts.getStyleClass().remove("admin-nav-item-active");
    btnBidders.getStyleClass().remove("admin-nav-item-active");
    btnSellers.getStyleClass().remove("admin-nav-item-active");
    btnTransactions.getStyleClass().remove("admin-nav-item-active");
    btnInvoices.getStyleClass().remove("admin-nav-item-active");
  }

  @FXML
  private void handleLogout() {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Xác nhận đăng xuất");
    alert.setHeaderText(null);
    alert.setContentText("Bạn có chắc chắn muốn đăng xuất không?");

    if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
      return;
    }
    logger.info("Admin da dang xuat.");
    try {
      Stage stage = (Stage) logOut.getScene().getWindow();
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auth/Login.fxml"));
      Parent root = loader.load();
      stage.getScene().setRoot(root);
      stage.setTitle("Đăng nhập hệ thống");
    } catch (IOException e) {
      logger.error("Loi khi chuyen ve man hinh dang nhap.", e);
    }
  }
}
