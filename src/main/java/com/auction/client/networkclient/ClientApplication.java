package com.auction.client.networkclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        //lôi file Khung xương FXML ra
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/auth/Login.fxml"));
        Scene scene = new Scene(root, 800, 600);

        // Đặt tên cho cửa sổ ứng dụng và nhét Scene vào
        primaryStage.setTitle("AuctionHub - Bảng Điều Khiển");
        primaryStage.setScene(scene);

        //  Ép cửa sổ bung tràn toàn màn hình ngay khi vừa khởi động
        primaryStage.setMaximized(true);

        // Hiển thị cửa sổ lên màn hình
        primaryStage.show();
    }

    public static void main(String[] args) {
        //  Ép Java dùng chuẩn UTF-8 để hiện tiếng Việt và Emoji
        System.setProperty("file.encoding", "UTF-8");

        launch(args); // Nút kích nổ của JavaFX
    }
}