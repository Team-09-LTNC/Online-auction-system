package com.auction.client.controller.seller;

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

public class MyProductsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MyProductsController.class);

    @FXML private FlowPane productFlowPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Người bán đang xem danh sách sản phẩm của chính mình.");
        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadMyPostedProducts();
        }
    }

    private void loadMyPostedProducts() {
        for (int i = 0; i < 2; i++) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/ProductCard.fxml"));
                VBox card = loader.load();
                ProductCardController controller = loader.getController();

                // Dữ liệu giả: Món đã đăng bán
                controller.setProductData("Xe máy Honda SH 160i " + i, 110000000.0, "00:00:00", "Đã kết thúc");

                productFlowPane.getChildren().add(card);
            } catch (IOException e) {
                logger.error("Lỗi nạp Card trong MyProducts: {}", e.getMessage());
            }
        }
    }
}