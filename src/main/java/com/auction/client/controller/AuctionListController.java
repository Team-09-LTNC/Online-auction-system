package com.auction.client.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
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
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import com.auction.server.utils.DatabaseConnection;

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

        // CẤU HÌNH CỘT THỜI GIAN ĐỂ ĐẾM NGƯỢC
        colTime.setCellFactory(column -> {
            return new TableCell<AuctionItem, String>() {
                @Override
                protected void updateItem(String endTimeStr, boolean empty) {
                    super.updateItem(endTimeStr, empty);

                    if (empty || endTimeStr == null || endTimeStr.isEmpty()) {
                        setText(null);
                        setStyle("");
                    } else {
                        try {
                            // Cắt phần đuôi .0 nếu MySQL tự thêm vào
                            String cleanTimeStr = endTimeStr;
                            if (endTimeStr.endsWith(".0")) {
                                cleanTimeStr = endTimeStr.substring(0, endTimeStr.length() - 2);
                            }

                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            LocalDateTime endTime = LocalDateTime.parse(cleanTimeStr, formatter);
                            LocalDateTime now = LocalDateTime.now();

                            if (now.isAfter(endTime)) {
                                setText("Đã kết thúc");
                                setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            } else {
                                java.time.Duration duration = java.time.Duration.between(now, endTime);
                                long days = duration.toDays();
                                long hours = duration.toHoursPart();
                                long minutes = duration.toMinutesPart();
                                long seconds = duration.toSecondsPart();

                                String timeRemaining = "";
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
                            setText(endTimeStr); // Nếu lỗi format thì in ra chuỗi gốc
                            setStyle("");
                        }
                    }
                }
            };
        });

        // Khởi tạo list trống
        auctionList = FXCollections.observableArrayList();

        // GỌI HÀM LẤY DỮ LIỆU TỪ DATABASE Ở ĐÂY
        loadDataFromDatabase();

        // Gán sự kiện cho nút Đăng xuất
        btnLogout.setOnAction(event -> logout());

        // Thread cập nhật danh sách đấu giá mỗi 3 giây (Fake polling)
        Timeline timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(3), event -> {
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
            System.out.println("❌ Lỗi: Không lấy được dữ liệu từ Database. Check lại mạng xem!");
            e.printStackTrace();
        }
    }

    // Hàm xử lý đăng xuất
    private void logout() {
        try {
            System.out.println("Đang đăng xuất...");
            Stage stage = (Stage) btnLogout.getScene().getWindow();
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
        AuctionItem selectedItem = auctionTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Bạn chưa chọn phiên đấu giá nào!");
            return;
        }

        String status = selectedItem.getStatus();

        if ("OPEN".equals(status) || "RUNNING".equals(status)) {
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