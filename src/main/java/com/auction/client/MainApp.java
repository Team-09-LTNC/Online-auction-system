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
        URL fxmllocation = getClass().getResource("/fxml/Home.fxml");
        System.out.println("Đường dẫn file: " + fxmllocation);

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        stage.setTitle("HỆ THỐNG ĐẤU GIÁ");

        stage.setOpacity(0);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        stage.setOpacity(1);
    }

    public static void main(String[] args) {
        launch(args);
    }
}