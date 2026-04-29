package com.auction.client.controller;

import java.io.IOException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.auction.server.utils.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuctionListController {

    @FXML
    private TableView<AuctionItem> auctionTable;

    @FXML
    private TableColumn<AuctionItem, String> colName;

    @FXML
    private TableColumn<AuctionItem, String> colPrice;

    @FXML
    private TableColumn<AuctionItem, String> colStatus;

    @FXML
    private TableColumn<AuctionItem, String> colTime;

    @FXML
    private Button btnLogout;

    @FXML
    private Button btnEnterAuction;

    // List chứa dữ liệu để đưa lên bảng
    private ObservableList<AuctionItem> auctionList;

    @FXML
    public void initialize() {
        System.out.println("Vào màn hình danh sách đấu giá...");

        // Set up các cột trong TableView
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timeLeft"));

        // Khởi tạo list trống
        auctionList = FXCollections.observableArrayList();

        // GỌI HÀM LẤY DỮ LIỆU TỪ DATABASE Ở ĐÂY
        loadDataFromDatabase();
        // Gán sự kiện cho nút Đăng xuất
        btnLogout.setOnAction(event -> logout());

        // Thread cập nhật danh sách đấu giá mỗi 3 giây (Fake polling)
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            // System.out.println("Đang giả lập gọi Server lấy data...");
            // TODO: Sau này học bài mạng xong sẽ gọi SocketClient.listAuctions() ở đây
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    // Hàm lôi dữ liệu THẬT từ MySQL Cloud
    private void loadDataFromDatabase() {
        auctionList.clear(); // Dọn sạch bảng trước khi tải

        String sql = "SELECT * FROM items";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                //
                String name = rs.getString("name");
                String price = rs.getString("current_price");
                String status = rs.getString("status");
                String timeLeft = rs.getString("end_time");

                auctionList.add(new AuctionItem(name, price, status, timeLeft));
            }

            // Đổ list lên bảng giao diện
            auctionTable.setItems(auctionList);
            System.out.println(">>> Đã tải thành công dữ liệu từ Cloud Database!");

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: Không lấy được dữ liệu từ Database.Check lại mạng xem!");
            e.printStackTrace(); // In ra lỗi chữ đỏ để biết sai ở đâu
        }
    }

    // Hàm xử lý đăng xuất
    private void logout() {
        try {
            System.out.println("Đang đăng xuất...");
            // Lấy Stage hiện tại từ nút bấm
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            // Load lại màn hình Login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setTitle("HỆ THỐNG ĐẤU GIÁ");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể quay lại màn hình đăng nhập!");
        }
    }

    @FXML
    void onEnterAuctionClick(ActionEvent event) {
        // Lấy object mà người dùng đang click chọn trong bảng
        AuctionItem selectedItem = auctionTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Bạn chưa chọn phiên đấu giá nào!");
            return;
        }

        String status = selectedItem.getStatus();

        //Trạng thái OPEN -> RUNNING -> FINISHED -> PAID / CANCELED
        // Chỉ cho vào xem/đấu giá nếu đang OPEN hoặc RUNNING
        if (status.equals("OPEN") || status.equals("RUNNING")) {
            System.out.println("Chuyển sang giao diện đấu giá của: " + selectedItem.getName());
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đang vào phiên đấu giá: " + selectedItem.getName() + " ...");
            // TODO: Chuyển Scene sang RealtimeBidding.fxml
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Không thể vào! Phiên đấu giá này đang ở trạng thái: " + status);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Class Model nội bộ
    // (TODO: Nhớ tách class này ra thư mục model lúc rảnh)
    public static class AuctionItem {
        private String name;
        private String price;
        private String status;
        private String timeLeft;

        public AuctionItem(String name, String price, String status, String timeLeft) {
            this.name = name;
            this.price = price;
            this.status = status;
            this.timeLeft = timeLeft;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getTimeLeft() {
            return timeLeft;
        }

        public void setTimeLeft(String timeLeft) {
            this.timeLeft = timeLeft;
        }
    }
}