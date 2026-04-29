package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeController {

    @FXML
    private Hyperlink linkSignIn;

    // Khi người dùng bấm nút Đăng nhập trên góc trái màn hình
    @FXML
    void onSignInClick(ActionEvent event) {
        try {
            System.out.println("Đang chuyển sang màn hình Đăng nhập...");

            // Lấy cửa sổ (Stage) hiện tại
            Stage stage = (Stage) linkSignIn.getScene().getWindow();

            // Tải file Login.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();

            // Chuyển cảnh sang form đăng nhập
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng nhập hệ thống");
            stage.show();

        } catch (IOException e) {
            System.out.println("Lỗi không tìm thấy file Login.fxml!");
            e.printStackTrace();
        }
    }
}