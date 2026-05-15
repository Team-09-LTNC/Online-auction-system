package com.auction.client.controller.bidder;

import com.auction.client.controller.components.ProductCardController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAuctionsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MyAuctionsController.class);

    @FXML private FlowPane productFlowPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Đang nạp danh sách các phiên đã tham gia đấu giá.");
        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadMyBids();
        }
    }

    private void loadMyBids() {
        for (int i = 0; i < 4; i++) { // Hiện khoảng 4 món thôi cho thật
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                VBox card = loader.load();
                ProductCardController controller = loader.getController();

                // Dữ liệu giả: Những món đang dẫn đầu hoặc đã tham gia
                controller.setProductData("Đồng hồ Rolex Submariner " + i, 450000000.0, "00:30:15", "Đang dẫn đầu");

                productFlowPane.getChildren().add(card);
            } catch (IOException e) {
                logger.error("Lỗi nạp Card trong MyAuctions: {}", e.getMessage());
            }
        }
    }
}