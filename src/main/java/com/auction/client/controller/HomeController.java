package com.auction.client.controller;

import com.auction.common.model.user.User;
import com.auction.server.utils.DatabaseConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    // =====================================================================
    // SESSION SINGLETON — dùng chung toàn app để lưu user đang đăng nhập
    // =====================================================================
    public static class Session {
        private static User currentUser = null;

        public static void login(User user) {
            currentUser = user;
        }

        public static void logout() {
            currentUser = null;
        }

        public static User getCurrentUser() {
            return currentUser;
        }

        public static boolean isLoggedIn() {
            return currentUser != null;
        }
    }

    // =====================================================================
    // FXML FIELDS
    // =====================================================================
    @FXML private Label lblGreeting;        // "Xin chào!" / "Xin chào, [tên] ([vai trò])"
    @FXML private Hyperlink linkSignIn;     // ẩn sau khi đăng nhập
    @FXML private Button btnLogout;         // hiện sau khi đăng nhập
    @FXML private Hyperlink linkPostAsset;  // "Đăng tài sản"
    @FXML private TextField searchField;    // ô tìm kiếm
    @FXML private FlowPane productFlow;     // chứa các card sản phẩm
    @FXML private Label lblSectionTitle;    // "Tài sản nổi bật" / "Kết quả tìm kiếm"

    // =====================================================================
    // INITIALIZE
    // =====================================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        refreshAuthBar();
        loadTopItems(null);
    }

    // =====================================================================
    // CẬP NHẬT THANH ĐĂNG NHẬP
    // =====================================================================
    public void refreshAuthBar() {
        if (Session.isLoggedIn()) {
            User u = Session.getCurrentUser();
            lblGreeting.setText("Xin chào, " + u.getFullName() + " (" + u.getRoleName() + ")");
            linkSignIn.setVisible(false);
            linkSignIn.setManaged(false);
            btnLogout.setVisible(true);
            btnLogout.setManaged(true);
        } else {
            lblGreeting.setText("Xin chào!");
            linkSignIn.setVisible(true);
            linkSignIn.setManaged(true);
            btnLogout.setVisible(false);
            btnLogout.setManaged(false);
        }
    }

    // =====================================================================
    // ĐĂNG NHẬP
    // =====================================================================
    @FXML
    void onSignInClick() {
        try {
            Stage stage = (Stage) linkSignIn.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();

            stage.getScene().setRoot(root);
            stage.setTitle("Đăng nhập hệ thống");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // =====================================================================
    // ĐĂNG XUẤT
    // =====================================================================
    @FXML
    void onLogoutClick() {
        Session.logout();
        refreshAuthBar();
        loadTopItems(null);
        if (lblSectionTitle != null) lblSectionTitle.setText("Tài sản nổi bật");
        if (searchField != null) searchField.clear();
        showInfo("Đã đăng xuất thành công.");
    }

    // =====================================================================
    // TÌM KIẾM
    // =====================================================================
    @FXML
    void onSearchClick() {
        String keyword = (searchField != null) ? searchField.getText().trim() : "";
        if (keyword.isEmpty()) {
            if (lblSectionTitle != null) lblSectionTitle.setText("Tài sản nổi bật");
            loadTopItems(null);
        } else {
            if (lblSectionTitle != null) lblSectionTitle.setText("Kết quả tìm kiếm: \"" + keyword + "\"");
            loadTopItems(keyword);
        }
    }

    // =====================================================================
    // XEM NGAY — chuyển sang AuctionListScreen
    // =====================================================================
    @FXML
    void onXemNgayClick() {
        try {
            Stage stage = (Stage) productFlow.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AuctionListScreen.fxml"));
            Parent root = loader.load();
            AuctionListController ctrl = loader.getController();
            ctrl.loadDataFromDatabase();
            stage.getScene().setRoot(root);
            stage.setTitle("HỆ THỐNG ĐẤU GIÁ - DANH SÁCH SẢN PHẨM");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =====================================================================
    // ĐĂNG TÀI SẢN
    // =====================================================================
    @FXML
    void onPostAssetClick() {
        if (!Session.isLoggedIn()) {
            showAlert(Alert.AlertType.WARNING, "Chưa đăng nhập",
                    "Bạn cần đăng nhập với vai trò Seller để đăng tài sản!");
            return;
        }
        User u = Session.getCurrentUser();
        if (!"SELLER".equalsIgnoreCase(u.getRoleName())) {
            showAlert(Alert.AlertType.WARNING, "Không có quyền",
                    "Chỉ tài khoản Seller mới có thể đăng tài sản!\nVai trò hiện tại của bạn: " + u.getRoleName());
            return;
        }
        try {
            Stage stage = (Stage) linkPostAsset.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SellerDashboard.fxml"));
            Parent root = loader.load();
            SellerDashboardController ctrl = loader.getController();
            ctrl.initSeller(u);
            stage.getScene().setRoot(root);
            stage.setTitle("HỆ THỐNG ĐẤU GIÁ - QUẢN LÝ SẢN PHẨM (SELLER)");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =====================================================================
    // TẢI TOP 10 SẢN PHẨM (hoặc tìm kiếm theo keyword)
    // =====================================================================
    private void loadTopItems(String keyword) {
        if (productFlow == null) return;
        productFlow.getChildren().clear();

        List<ItemData> items = fetchItemsFromDb(keyword);
        if (items.isEmpty()) {
            Label empty = new Label("Không có sản phẩm nào.");
            empty.setStyle("-fx-text-fill: #8b6354; -fx-font-size: 14px; -fx-font-style: italic;");
            productFlow.getChildren().add(empty);
            return;
        }

        for (ItemData item : items) {
            productFlow.getChildren().add(buildItemCard(item));
        }
    }

    // =====================================================================
    // TRUY VẤN DATABASE
    // =====================================================================
    private List<ItemData> fetchItemsFromDb(String keyword) {
        List<ItemData> list = new ArrayList<>();

        String sql;
        if (keyword == null || keyword.isEmpty()) {
            // Top 10 theo số lượt đấu giá cao nhất
            sql = "SELECT i.id, i.name, i.category, i.current_price, i.status, " +
                  "COUNT(bt.id) AS bid_count " +
                  "FROM items i " +
                  "LEFT JOIN auctions a ON i.id = a.item_id " +
                  "LEFT JOIN bid_transaction bt ON a.id = bt.auction_id " +
                  "GROUP BY i.id, i.name, i.category, i.current_price, i.status " +
                  "ORDER BY bid_count DESC, i.id DESC " +
                  "LIMIT 10";
        } else {
            sql = "SELECT i.id, i.name, i.category, i.current_price, i.status, " +
                  "COUNT(bt.id) AS bid_count " +
                  "FROM items i " +
                  "LEFT JOIN auctions a ON i.id = a.item_id " +
                  "LEFT JOIN bid_transaction bt ON a.id = bt.auction_id " +
                  "WHERE i.name LIKE ? OR i.description LIKE ? OR i.category LIKE ? " +
                  "GROUP BY i.id, i.name, i.category, i.current_price, i.status " +
                  "ORDER BY bid_count DESC, i.id DESC " +
                  "LIMIT 20";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (keyword != null && !keyword.isEmpty()) {
                String like = "%" + keyword + "%";
                pstmt.setString(1, like);
                pstmt.setString(2, like);
                pstmt.setString(3, like);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new ItemData(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getDouble("current_price"),
                            rs.getString("status"),
                            rs.getInt("bid_count")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi tải danh sách sản phẩm: " + e.getMessage());
            // Fallback: hiển thị card mẫu khi không có DB
            list.add(new ItemData(0, "Tranh Mona Lisa (Bản sao)", "ART", 5000.0, "OPEN", 0));
            list.add(new ItemData(0, "Đồng hồ Rolex cổ", "ELECTRONICS", 12500.0, "RUNNING", 0));
            list.add(new ItemData(0, "Bình gốm thời Minh", "ART", 8200.0, "OPEN", 0));
        }
        return list;
    }

    // =====================================================================
    // XÂY DỰNG CARD SẢN PHẨM
    // =====================================================================
    private VBox buildItemCard(ItemData item) {
        VBox card = new VBox(10);
        card.setPrefWidth(200);
        card.setPrefHeight(230);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.10), 10, 0, 0, 4); " +
                "-fx-padding: 18; -fx-alignment: CENTER;");

        // Icon theo loại
        Label icon = new Label(getCategoryIcon(item.category));
        icon.setStyle("-fx-font-size: 32px;");

        // Tên
        Label name = new Label(item.name);
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1A0F0A; -fx-wrap-text: true; -fx-text-alignment: center;");
        name.setMaxWidth(180);
        name.setWrapText(true);

        // Giá
        Label price = new Label(String.format("%.0f$", item.currentPrice));
        price.setStyle("-fx-text-fill: #8b6354; -fx-font-size: 13px; -fx-font-weight: bold;");

        // Lượt đấu giá
        Label bids = new Label("🔥 " + item.bidCount + " lượt đấu giá");
        bids.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

        // Trạng thái
        Label status = new Label(item.status);
        String statusColor = "RUNNING".equals(item.status) ? "#27ae60" :
                             "FINISHED".equals(item.status) ? "#e74c3c" : "#e67e22";
        status.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        // Nút đấu giá
        Button btnBid = new Button("Đấu giá ngay");
        btnBid.setStyle("-fx-background-color: #8b6354; -fx-text-fill: white; " +
                "-fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 12px; " +
                "-fx-padding: 6 16 6 16;");
        btnBid.setOnAction(e -> handleBidClick(item, btnBid));

        card.getChildren().addAll(icon, name, price, bids, status, btnBid);
        return card;
    }

    // =====================================================================
    // XỬ LÝ CLICK "ĐẤU GIÁ NGAY"
    // =====================================================================
    private void handleBidClick(ItemData item, Button btn) {
        if (!Session.isLoggedIn()) {
            showAlert(Alert.AlertType.WARNING, "Chưa đăng nhập",
                    "Bạn phải đăng nhập để tiếp tục đấu giá!");
            return;
        }

        if (!"OPEN".equals(item.status) && !"RUNNING".equals(item.status)) {
            showAlert(Alert.AlertType.ERROR, "Không thể tham gia",
                    "Phiên đấu giá này đã kết thúc hoặc bị hủy!\nTrạng thái: " + item.status);
            return;
        }

        // Disable nút để tránh click nhiều lần
        btn.setDisable(true);
        btn.setText("Đang vào...");

        showAlert(Alert.AlertType.INFORMATION, "Đang kết nối",
                "Đang vào phiên đấu giá: " + item.name + "...");

        // Giả lập xử lý nhẹ rồi thông báo thành công
        new Thread(() -> {
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                btn.setText("✅ Đã vào");
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Vào phiên đấu giá thành công!\n📦 " + item.name);
                // TODO: Chuyển sang màn hình RealtimeBidding.fxml sau khi có
            });
        }).start();
    }

    // =====================================================================
    // HELPER
    // =====================================================================
    private String getCategoryIcon(String category) {
        if (category == null) return "📦";
        return switch (category.toUpperCase()) {
            case "ART" -> "🎨";
            case "ELECTRONICS" -> "💻";
            case "VEHICLE" -> "🚗";
            default -> "📦";
        };
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }

    // =====================================================================
    // DATA CLASS
    // =====================================================================
    public static class ItemData {
        public final int id;
        public final String name;
        public final String category;
        public final double currentPrice;
        public final String status;
        public final int bidCount;

        public ItemData(int id, String name, String category, double currentPrice,
                        String status, int bidCount) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.currentPrice = currentPrice;
            this.status = status;
            this.bidCount = bidCount;
        }
    }
}
