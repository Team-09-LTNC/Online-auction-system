package com.auction.client.controller.bidder;

import com.auction.client.controller.components.ProductCardController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class FollowedAuctionsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(FollowedAuctionsController.class);

    @FXML private FlowPane productFlowPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Đang nạp danh sách sản phẩm đang theo dõi.");
        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadFollowedAuctions();
        }
    }

    private void loadFollowedAuctions() {
        for (int i = 0; i < 3; i++) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                VBox card = loader.load();
                ProductCardController controller = loader.getController();

                // Dữ liệu giả cho mục Theo dõi
                controller.setProductData("MacBook Pro M3 Max " + i, 89000000.0, "05:12:00", "Sắp kết thúc");

                productFlowPane.getChildren().add(card);
            } catch (IOException e) {
                logger.error("Lỗi nạp Card trong Followed: {}", e.getMessage());
            }
        }
    }
}