package com.auction.client;

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

        // Tạo một Scene (cái nền) kích thước 1280x800 chứa cái khung xương đó
//        Scene scene = new Scene(root, 1280, 800);
        Scene scene = new Scene(root, 800, 600);

        // Đặt tên cho cửa sổ ứng dụng và nhét Scene vào
        primaryStage.setTitle("AuctionHub - Bảng Điều Khiển");
        primaryStage.setScene(scene);

        // Hiển thị cửa sổ lên màn hình
        primaryStage.show();
    }

    public static void main(String[] args) {
        //  Ép Java dùng chuẩn UTF-8 để hiện tiếng Việt và Emoji
        System.setProperty("file.encoding", "UTF-8");

        launch(args); // Nút kích nổ của JavaFX
    }
}