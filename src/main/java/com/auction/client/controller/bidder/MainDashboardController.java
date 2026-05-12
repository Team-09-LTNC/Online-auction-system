package com.auction.client.controller.bidder;

import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainDashboardController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainDashboardController.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Hàm này tự động chạy khi giao diện được mở lên
        logger.info("Bidder đã vào Dashboard chính.");
    }
}