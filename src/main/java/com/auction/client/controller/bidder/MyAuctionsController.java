package com.auction.client.controller.bidder;

import com.auction.client.controller.auth.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class MyAuctionsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MyAuctionsController.class);

    // Khai báo các nhãn Header để nhận diện từ FXML
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Gọi hàm đồng bộ dữ liệu thật khi tab được nạp
        updateHeaderUserInfo();

        // Luồng xử lý lấy danh sách phòng đấu giá cá nhân của bạn viết tiếp ở đây...
    }

    private void updateHeaderUserInfo() {
        try {
            String currentUserName = UserSession.getUsername() != null ? UserSession.getUsername() : "Người dùng";
            String currentUserRole = UserSession.getCurrentRole() != null ? UserSession.getCurrentRole() : "BIDDER";

            if (lblHeaderName != null) lblHeaderName.setText("Chào, " + currentUserName);
            if (lblHeaderRole != null) {
                lblHeaderRole.setText(currentUserRole.substring(0, 1).toUpperCase() + currentUserRole.substring(1).toLowerCase());
            }
        } catch (Exception e) {
            logger.error("Lỗi cập nhật thông tin Header tại MyAuctions: {}", e.getMessage());
        }
    }
}