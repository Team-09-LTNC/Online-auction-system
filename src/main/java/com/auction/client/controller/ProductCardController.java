package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class ProductCardController {
    @FXML private ImageView imgProduct;
    @FXML private Label lblProductName;
    @FXML private Label lblCurrentBid;
    @FXML private Button btnBid;

    public void setData(String name, double price) {
        lblProductName.setText(name);
        lblCurrentBid.setText(String.format("%,.0f VNĐ", price));

    }
}


