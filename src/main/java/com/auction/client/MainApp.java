package com.auction.client;

import java.io.IOException;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Đổi thành Home.fxml để check đường dẫn
        URL fxmllocation = getClass().getResource("/fxml/Home.fxml");
        System.out.println("Đường dẫn file: " + fxmllocation);

        // Nạp file giao diện Trang chủ (Home.fxml)
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        stage.setTitle("HỆ THỐNG ĐẤU GIÁ");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}