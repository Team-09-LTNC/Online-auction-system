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

public class MainDashboardController implements Initializable, com.auction.client.interfaces.CategoryFilterListener {

    private static final Logger logger = LoggerFactory.getLogger(MainDashboardController.class);

    @FXML private FlowPane productFlowPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Bidder đã vào Dashboard chính - Đang nạp danh sách sản phẩm.");

        // Xóa sạch các card cũ (nếu có) trước khi nạp mới
        if (productFlowPane != null) {
            productFlowPane.getChildren().clear();
            loadMockProducts();
        }
    }

    private void loadMockProducts() {
        // muốn hiện bao nhiêu sản phẩm cũng được, FlowPane tự xếp
        for (int i = 0; i < 12; i++) {
            try {
                // 2. Nạp file FXML của cái Card sản phẩm
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/fxml/components/ProductCard.fxml"));
                VBox card = loader.load();

                // 3. Lấy Controller để đổ dữ liệu giả
                ProductCardController controller = loader.getController();

                if (i % 2 == 0) {
                    controller.setProductData("Mercedes-Benz S450 " + i, 3500000000.0, "02:15:30", "Đang diễn ra");
                } else {
                    controller.setProductData("iPhone 15 Pro Max " + i, 32000000.0, "00:00:00", "Đã kết thúc");
                }

                productFlowPane.getChildren().add(card);

            } catch (IOException e) {
                logger.error("Không nạp được ProductCard.fxml: {}", e.getMessage());
            }
        }
    }
    @Override
    public void onCategorySelected(String category) {
        System.out.println("LOG: Dashboard đang thực hiện lọc cho danh mục: " + category);

        productFlowPane.getChildren().clear();
        loadMockProducts();
    }
}