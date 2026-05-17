package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainController {
    public static MainController instance;

    @FXML
    private StackPane mainContentArea;

    // Biến này để lưu Controller của trang đang hiện ở Center (ví dụ DashboardController)
    private Object currentCenterController;

    public void initialize() {
        instance = this;
    }

    public void setCenterContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newNode = loader.load();

            // QUAN TRỌNG: Lấy Controller của trang vừa load
            currentCenterController = loader.getController();

            mainContentArea.getChildren().setAll(newNode);

        } catch (IOException e) {
            System.err.println("Lỗi load FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // Hàm để các Controller khác lấy được cái Controller đang hiện ở giữa
    public Object getCurrentCenterController() {
        return currentCenterController;
    }
}