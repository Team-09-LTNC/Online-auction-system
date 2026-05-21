package com.auction.client.controller.admin;

import com.auction.client.manager.AdminManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * DashboardViewController
 * ─────────────────────────────────────────────────────────────
 * Controller cho DashboardView.fxml.
 * Hiển thị 3 chỉ số: số người đấu giá, người bán, phiên đấu giá.
 */
public class DashboardViewController implements Initializable {

    @FXML
    private StackPane contentPane;
    @FXML
    private Label lblBidderCount;
    @FXML
    private Label lblSellerCount;
    @FXML
    private Label lblAuctionCount;
    @FXML
    private Button btnRefresh;
    @FXML
    private TextField globalSearch;

    private static final Logger logger = LoggerFactory.getLogger(DashboardViewController.class);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadStats();
    }

    // ── Load dữ liệu ─────────────────────────────────────────

    private void loadStats() {
        // TODO: thay bằng lời gọi Service / DAO thực tế
        AdminManager.getInstance().layTongSoBidder(
                count -> Platform.runLater(() -> lblBidderCount.setText(String.valueOf(count))),
                error -> System.err.println("Lỗi: " + error));
        AdminManager.getInstance().layTongSoSeller(
                count -> Platform.runLater(() -> lblSellerCount.setText(String.valueOf(count))),
                error -> System.err.println("Lỗi: " + error));
    }

    // ── FXML handlers ────────────────────────────────────────

    @FXML
    private void handleRefresh() {
        loadStats();
    }

    // ── Public API ───────────────────────────────────────────

    /** Cập nhật từng chỉ số thủ công (dùng khi có broadcast từ nơi khác). */
    public void updateCounts(int bidders, int sellers, int auctions) {
        lblBidderCount.setText(String.valueOf(bidders));
        lblSellerCount.setText(String.valueOf(sellers));
        lblAuctionCount.setText(String.valueOf(auctions));
    }

    @FXML
    private void handleGlobalSearch() {
        String query = globalSearch.getText();
        logger.info("Admin thực hiện tìm kiếm toàn cục với từ khóa: {}", query);

    }
}
