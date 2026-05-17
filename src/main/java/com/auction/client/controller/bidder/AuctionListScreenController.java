package com.auction.client.controller.bidder;

import com.auction.client.controller.MainController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class AuctionListScreenController implements Initializable {

    // --- BIẾN GIAO DIỆN  ---
    @FXML private TableView<AuctionModel> auctionTable;
    @FXML private TableColumn<AuctionModel, String> colName;
    @FXML private TableColumn<AuctionModel, String> colPrice;
    @FXML private TableColumn<AuctionModel, String> colStatus;
    @FXML private TableColumn<AuctionModel, String> colTime;
    @FXML private Button btnBack;
    @FXML private Button btnEnterAuction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Định nghĩa cách các cột lấy dữ liệu từ Model
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timeRemaining"));

        // 2. Nạp dữ liệu giả để thầy thấy bảng không bị trống
        loadMockData();
    }

    private void loadMockData() {
        auctionTable.getItems().add(new AuctionModel("Laptop Dell XPS 15", "25,000,000 đ", "Đang diễn ra", "01:45:00"));
        auctionTable.getItems().add(new AuctionModel("Máy ảnh Sony A7III", "35,000,000 đ", "Đang diễn ra", "00:20:10"));
        auctionTable.getItems().add(new AuctionModel("Đồng hồ Apple Watch", "8,000,000 đ", "Đã kết thúc", "00:00:00"));
    }

    // Xử lý khi bấm nút "Quay về" (onAction="#onBackClick")
    @FXML
    private void onBackClick() {
        // Quay lại trang Dashboard chính của Bidder
        MainController.instance.setCenterContent("/fxml/bidder/MainDashboard.fxml");
    }

    // Xử lý khi bấm nút "VÀO ĐẤU GIÁ" (onAction="#onEnterAuctionClick")
    @FXML
    private void onEnterAuctionClick() {
        AuctionModel selected = auctionTable.getSelectionModel().getSelectedItem();

        if (selected != null) {
            System.out.println("Vào phòng cho sản phẩm: " + selected.getName());
            // Dẫn user vào cái AuctionRoom (Cái Mercedes m vừa làm xong ấy)
            MainController.instance.setCenterContent("/fxml/bidder/AuctionRoom.fxml");
        } else {
            // Nếu chưa chọn dòng nào mà đã bấm nút
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText("M phải chọn một phiên đấu giá trong bảng trước khi bấm nút nhé!");
            alert.showAndWait();
        }
    }

    // --- CLASS MODEL PHỤ (Dùng để chứa dữ liệu cho mỗi dòng trong bảng) ---
    public static class AuctionModel {
        private String name, price, status, timeRemaining;

        public AuctionModel(String name, String price, String status, String timeRemaining) {
            this.name = name;
            this.price = price;
            this.status = status;
            this.timeRemaining = timeRemaining;
        }

        // CỰC QUAN TRỌNG: Phải có Getter thì TableView mới hiện chữ được
        public String getName() { return name; }
        public String getPrice() { return price; }
        public String getStatus() { return status; }
        public String getTimeRemaining() { return timeRemaining; }
    }
}