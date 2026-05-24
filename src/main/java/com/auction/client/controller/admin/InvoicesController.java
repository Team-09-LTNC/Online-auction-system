package com.auction.client.controller.admin;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

import com.auction.client.manager.AdminManager;

/**
 * InvoicesController
 * ─────────────────────────────────────────────────────────────
 * Controller cho InvoicesView.fxml.
 * Hiển thị danh sách sản phẩm đã thanh toán (status = PAID).
 */
public class InvoicesController implements Initializable {

    // ── FXML injections ──────────────────────────────────────
    @FXML
    private StackPane contentPane;
    @FXML
    private TableView<Invoice> invoiceTable;
    @FXML
    private TableColumn<Invoice, String> colProductId;
    @FXML
    private TableColumn<Invoice, String> colName;
    @FXML
    private TableColumn<Invoice, String> colAuctionId;
    @FXML
    private TableColumn<Invoice, String> colSellerId;
    @FXML
    private TableColumn<Invoice, String> colWinnerId;
    @FXML
    private TableColumn<Invoice, String> colFinalPrice;
    @FXML
    private Label lblInvoiceCount;
    @FXML
    private Label lblTotalRevenue;
    @FXML
    private TextField tfSearch;

    // ── Data ─────────────────────────────────────────────────
    private final ObservableList<Invoice> masterList = FXCollections.observableArrayList();
    private FilteredList<Invoice> filteredList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        setupTable();
        loadData();
    }

    // ── Setup ────────────────────────────────────────────────

    private void setupColumns() {
        colProductId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductId()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colAuctionId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAuctionId()));
        colSellerId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSellerId()));
        colWinnerId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getWinnerId()));
        colFinalPrice.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFinalPrice()));
    }

    private void setupTable() {
        filteredList = new FilteredList<>(masterList, p -> true);
        invoiceTable.setItems(filteredList);
        invoiceTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void loadData() {
        AdminManager.getInstance().layDanhSachHoaDon(
                invoices -> Platform.runLater(() -> {
                    masterList.clear();
                    invoices.forEach(inv -> masterList.add(new Invoice(
                            String.valueOf(inv.getItemId()), // itemId → productId
                            inv.getItemName(), // itemName → name
                            String.valueOf(inv.getAuctionId()),
                            String.valueOf(inv.getSellerId()),
                            String.valueOf(inv.getWinnerId()),
                            String.format("%,d", inv.getHighestBid()) // highestBid → finalPrice
                    )));
                    updateSummary();
                }),
                error -> Platform.runLater(() -> lblInvoiceCount.setText("Lỗi: " + error)),
                tongDoanhThu -> Platform
                        .runLater(() -> lblTotalRevenue.setText("Tổng: ₫ " + String.format("%,d", tongDoanhThu))));
    }

    // ── FXML handlers ────────────────────────────────────────

    @FXML
    private void handleSearch() {
        String kw = tfSearch.getText().trim().toLowerCase();
        filteredList.setPredicate(inv -> kw.isEmpty()
                || inv.getProductId().toLowerCase().contains(kw)
                || inv.getName().toLowerCase().contains(kw)
                || inv.getAuctionId().toLowerCase().contains(kw)
                || inv.getSellerId().toLowerCase().contains(kw)
                || inv.getWinnerId().toLowerCase().contains(kw));
        updateSummary();
    }

    // ── Helpers ──────────────────────────────────────────────

    private void updateSummary() {
        int shown = filteredList.size();
        int total = masterList.size();
        lblInvoiceCount.setText(shown == total
                ? total + " hoá đơn"
                : shown + " / " + total + " hoá đơn");
        // Bỏ phần tính sum — đã có server tính sẵn qua callback tongDoanhThu
    }

    // ── Model ────────────────────────────────────────────────

    public static class Invoice {
        private final SimpleStringProperty productId;
        private final SimpleStringProperty name;
        private final SimpleStringProperty auctionId;
        private final SimpleStringProperty sellerId;
        private final SimpleStringProperty winnerId;
        private final SimpleStringProperty finalPrice;

        public Invoice(String productId, String name, String auctionId,
                String sellerId, String winnerId, String finalPrice) {
            this.productId = new SimpleStringProperty(productId);
            this.name = new SimpleStringProperty(name);
            this.auctionId = new SimpleStringProperty(auctionId);
            this.sellerId = new SimpleStringProperty(sellerId);
            this.winnerId = new SimpleStringProperty(winnerId);
            this.finalPrice = new SimpleStringProperty(finalPrice);
        }

        public String getProductId() {
            return productId.get();
        }

        public String getName() {
            return name.get();
        }

        public String getAuctionId() {
            return auctionId.get();
        }

        public String getSellerId() {
            return sellerId.get();
        }

        public String getWinnerId() {
            return winnerId.get();
        }

        public String getFinalPrice() {
            return finalPrice.get();
        }

        public SimpleStringProperty productIdProperty() {
            return productId;
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public SimpleStringProperty auctionIdProperty() {
            return auctionId;
        }

        public SimpleStringProperty sellerIdProperty() {
            return sellerId;
        }

        public SimpleStringProperty winnerIdProperty() {
            return winnerId;
        }

        public SimpleStringProperty finalPriceProperty() {
            return finalPrice;
        }
    }
}
