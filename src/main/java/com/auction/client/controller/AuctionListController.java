package com.auction.client.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import com.auction.server.utils.DatabaseConnection;

public class AuctionListController {

    @FXML private TableView<AuctionItem>           auctionTable;
    @FXML private TableColumn<AuctionItem, String> colName;
    @FXML private TableColumn<AuctionItem, String> colPrice;
    @FXML private TableColumn<AuctionItem, String> colStatus;
    @FXML private TableColumn<AuctionItem, String> colTime;
    @FXML private Button                           btnBack;
    @FXML private Button                           btnEnterAuction;

    private ObservableList<AuctionItem> auctionList;

    @FXML
    public void initialize() {
        System.out.println("Vào màn hình danh sách đấu giá...");

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timeLeft"));

        // Cấu hình cột thời gian đếm ngược
        colTime.setCellFactory(column -> new TableCell<AuctionItem, String>() {
            @Override
            protected void updateItem(String endTimeStr, boolean empty) {
                super.updateItem(endTimeStr, empty);
                if (empty || endTimeStr == null || endTimeStr.isEmpty()) {
                    setText(null);
                    setStyle("");
                    return;
                }
                try {
                    String cleanTimeStr = endTimeStr.endsWith(".0")
                            ? endTimeStr.substring(0, endTimeStr.length() - 2)
                            : endTimeStr;

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime endTime = LocalDateTime.parse(cleanTimeStr, formatter);
                    LocalDateTime now     = LocalDateTime.now();

                    if (now.isAfter(endTime)) {
                        setText("Đã kết thúc");
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        java.time.Duration duration = java.time.Duration.between(now, endTime);
                        long days    = duration.toDays();
                        long hours   = duration.toHoursPart();
                        long minutes = duration.toMinutesPart();
                        long seconds = duration.toSecondsPart();

                        String timeRemaining;
                        if (days > 0) {
                            timeRemaining = days + " ngày " + hours + " giờ";
                        } else if (hours > 0) {
                            timeRemaining = hours + " giờ " + minutes + " phút";
                        } else {
                            timeRemaining = minutes + " phút " + seconds + " giây";
                        }

                        setText("Còn " + timeRemaining);
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    }
                } catch (Exception e) {
                    setText(endTimeStr);
                    setStyle("");
                }
            }
        });

        auctionList = FXCollections.observableArrayList();
        auctionTable.setItems(auctionList);
        loadDataFromDatabase();

        // Làm mới đếm ngược mỗi giây
        Timeline timeline = new Timeline(new KeyFrame(
                javafx.util.Duration.seconds(1),
                event -> auctionTable.refresh()
        ));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Hover effect cho nút back
        btnBack.setOnMouseEntered(e -> btnBack.setStyle(
                "-fx-background-color: rgba(255,255,255,0.30);" +
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 8 16 8 16; -fx-background-radius: 8;" +
                "-fx-border-color: rgba(255,255,255,0.8); -fx-border-radius: 8; -fx-border-width: 1.5;"
        ));
        btnBack.setOnMouseExited(e -> btnBack.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15);" +
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 8 16 8 16; -fx-background-radius: 8;" +
                "-fx-border-color: rgba(255,255,255,0.5); -fx-border-radius: 8; -fx-border-width: 1.5;"
        ));
    }

    // =========================================================================
    // TẢI DỮ LIỆU TỪ DATABASE
    // =========================================================================
    public void loadDataFromDatabase() {
        auctionList.clear();

        String sql = "SELECT i.name, i.current_price, i.status, a.end_time "
                   + "FROM items i "
                   + "LEFT JOIN auctions a ON i.id = a.item_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                auctionList.add(new AuctionItem(
                        rs.getString("name"),
                        rs.getString("current_price"),
                        rs.getString("status"),
                        rs.getString("end_time")
                ));
            }
            auctionTable.setItems(auctionList);
            System.out.println(">>> Đã tải " + auctionList.size() + " phiên từ Database!");

        } catch (SQLException e) {
            System.out.println("❌ Lỗi tải dữ liệu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    // NÚT ← QUAY VỀ HOME (giữ nguyên session đăng nhập)
    // =========================================================================
    @FXML
    private void onBackClick() {
        try {
            Stage stage = (Stage) btnBack.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"));
            Parent root = loader.load();

            // Refresh auth bar để giữ trạng thái đăng nhập
            HomeController homeCtrl = loader.getController();
            homeCtrl.refreshAuthBar();

            stage.getScene().setRoot(root);
            stage.setTitle("HỆ THỐNG ĐẤU GIÁ");

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể quay về trang chủ!");
        }
    }

    // =========================================================================
    // NÚT VÀO ĐẤU GIÁ
    // =========================================================================
    @FXML
    void onEnterAuctionClick(ActionEvent event) {
        AuctionItem selectedItem = auctionTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Bạn chưa chọn phiên đấu giá nào!");
            return;
        }

        String status = selectedItem.getStatus();
        if ("OPEN".equals(status) || "RUNNING".equals(status)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đang vào phiên đấu giá: " + selectedItem.getName() + " ...");
            // TODO: Chuyển sang RealtimeBidding.fxml
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại",
                    "Không thể vào! Phiên đấu giá đang ở trạng thái: " + status);
        }
    }

    // =========================================================================
    // HELPER
    // =========================================================================
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // =========================================================================
    // MODEL — dữ liệu hiển thị trong TableView
    // =========================================================================
    public static class AuctionItem {
        private String name;
        private String price;
        private String status;
        private String timeLeft;

        public AuctionItem(String name, String price, String status, String timeLeft) {
            this.name     = name;
            this.price    = price;
            this.status   = status;
            this.timeLeft = timeLeft;
        }

        public String getName()              { return name; }
        public void   setName(String v)      { this.name = v; }
        public String getPrice()             { return price; }
        public void   setPrice(String v)     { this.price = v; }
        public String getStatus()            { return status; }
        public void   setStatus(String v)    { this.status = v; }
        public String getTimeLeft()          { return timeLeft; }
        public void   setTimeLeft(String v)  { this.timeLeft = v; }
    }
}
