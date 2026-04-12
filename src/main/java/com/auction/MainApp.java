package com.auction;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException{
        //Kiểm tra xem nó có tìm thấy file không
        URL fxmllocation = getClass().getResource("/fxml/Login.fxml");
        System.out.println("Mày dò máy dò hihi: " + fxmllocation);
      // Tìm đường dẫn :resources/fxml/Login.fxml
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/fxml/Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("HỆ THỐNG ĐẤU GIÁ - ĐĂNG NHẬP");
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args){
        launch();
    }
}
