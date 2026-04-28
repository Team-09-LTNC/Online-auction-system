package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.event.ActionEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class AuctionListController {

    @FXML
    private TableView<?> auctionTable;

    @FXML
    private TableColumn<?, ?> colName;

    @FXML
    private TableColumn<?, ?> colPrice;

    @FXML
    private TableColumn<?, ?> colStatus;

    @FXML
    private TableColumn<?, ?> colTime;

    @FXML
    private Button btnLogout;

    @FXML
    private Button btnEnterAuction;

    @FXML
    public void initialize() {
        System.out.println(">>> Đã mở màn hình Danh sách đấu giá thành công!");
        // Cài đặt đồng hồ 3 giây làm mới 1 lần theo đúng yêu cầu
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            System.out.println("Đang kết nối Server làm mới danh sách đấu giá...");
            // TODO: Bỏ comment dòng dưới khi viết xong hàm listAuctions()
            // SocketClient.listAuctions();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

    }

    @FXML
    void onEnterAuctionClick(ActionEvent event) {
        System.out.println("M đang bấm nút vào đấu giá nè!");
    }
}