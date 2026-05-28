package com.auction.client.controller.admin;

import com.auction.client.interfaces.RefreshableCenterContent;
import com.auction.client.manager.AdminManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Bộ điều khiển TransactionsViewController
 * ─────────────────────────────────────────────────────────────
 * Bộ điều khiển cho TransactionsView.fxml.
 * Phiên đấu giá đã kết thúc; admin có thể thay đổi trạng thái.
 */
public class TransactionsViewController implements Initializable, RefreshableCenterContent {

    // ── Thành phần FXML được inject ───────────────────────────
    @FXML private StackPane                        contentPane;
    @FXML private TableView<Transaction>           txTable;
    @FXML private TableColumn<Transaction, String> colProductId;
    @FXML private TableColumn<Transaction, String> colStartTime;
    @FXML private TableColumn<Transaction, String> colEndTime;
    @FXML private TableColumn<Transaction, String> colStatus;
    @FXML private TableColumn<Transaction, String> colWinnerId;
    @FXML private TableColumn<Transaction, String> colFinalPrice;
    @FXML private Label                            lblTxCount;
    @FXML private TextField                        tfSearch;
    @FXML private ComboBox<String>                 cbStatusFilter;
    @FXML private Button                           btnChangeStatus;

    // ── Dữ liệu ───────────────────────────────────────────────
    private final ObservableList<Transaction> masterList   = FXCollections.observableArrayList();
    private       FilteredList<Transaction>  filteredList;

    /** Tuỳ chọn lọc trên ComboBox. */
    private static final String ALL_STATUS = "Tất cả";
    private static final List<String> FILTER_OPTIONS =
            List.of(ALL_STATUS, "FINISHED", "PAID", "CANCELED");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        setupStatusFilter();
        setupTable();
        loadData();
    }

    // ── Thiết lập ─────────────────────────────────────────────

    private void setupColumns() {
        colProductId .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductId()));
        colStartTime .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStartTime()));
        colEndTime   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEndTime()));
        colStatus    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colWinnerId  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getWinnerId()));
        colFinalPrice.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFinalPrice()));
    }

    private void setupStatusFilter() {
        cbStatusFilter.setItems(FXCollections.observableArrayList(FILTER_OPTIONS));
        cbStatusFilter.getSelectionModel().selectFirst();
    }

    private void setupTable() {
        filteredList = new FilteredList<>(masterList, p -> true);
        txTable.setItems(filteredList);
        txTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void loadData() {
        AdminManager.getInstance().getTransactions(
                transactions -> Platform.runLater(() -> {
                    masterList.clear();
                    transactions.forEach(tx -> masterList.add(new Transaction(
                            String.valueOf(tx.getAuctionId()),
                            String.valueOf(tx.getItemId()),
                            tx.getItemName(),
                            tx.getStartTime(),
                            tx.getEndTime(),
                            tx.getStatus(),
                            tx.getWinnerId() > 0 ? String.valueOf(tx.getWinnerId()) : "-",
                            String.format("%,d", tx.getFinalPrice()))));
                    applyFilter();
                }),
                error -> Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, error).showAndWait())
        );
    }

    @Override
    public void refreshContent() {
        loadData();
    }

    // ── Hàm xử lý FXML ────────────────────────────────────────

    @FXML
    private void handleTableClick(MouseEvent e) {
        boolean selected = txTable.getSelectionModel().getSelectedItem() != null;
        btnChangeStatus.setDisable(!selected);
    }

    @FXML
    private void handleSearch() {
        applyFilter();
    }

    @FXML
    private void handleFilter() {
        applyFilter();
    }

    /**
     * Mở dialog cho phép admin chọn trạng thái mới
     * và cập nhật lên bản ghi được chọn.
     */
    @FXML
    private void handleChangeStatus() {
        Transaction selected = txTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String currentStatus = selected.getStatus();
        List<String> choices;
        switch (currentStatus) {
            case "FINISHED":
                choices = List.of("PAID");
                break;
            case "CANCELED":
                choices = List.of("REOPEN");
                break;
            default:
                showInfo("Không thể thay đổi", "Phiên đang " + currentStatus + " không thể đổi trạng thái.");
                return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle("Thay đổi trạng thái");
        dialog.setHeaderText("Phiên: " + selected.getProductId());
        dialog.setContentText("Phiên đang " + currentStatus + ". Chọn hành động:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(action -> AdminManager.getInstance().changeAuctionStatus(
                Integer.parseInt(selected.getAuctionId()),
                action,
                response -> Platform.runLater(() -> {
                    String msg = response.has("message")
                            ? response.get("message").getAsString()
                            : "Đã cập nhật trạng thái!";
                    String actualStatus = response.has("newStatus")
                            ? response.get("newStatus").getAsString()
                            : action;
                    selected.setStatus(actualStatus);
                    if ("OPEN".equals(actualStatus) || "RUNNING".equals(actualStatus)) {
                        masterList.remove(selected);
                    }
                    txTable.refresh();
                    applyFilter();
                    showInfo("Cập nhật thành công", msg);
                }),
                error -> Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, error).showAndWait())));
    }

    // ── Hàm hỗ trợ ────────────────────────────────────────────

    private void applyFilter() {
        String kw     = tfSearch.getText().trim().toLowerCase();
        String status = cbStatusFilter.getValue();

        filteredList.setPredicate(tx -> {
            boolean matchKw = kw.isEmpty()
                    || tx.getProductId().toLowerCase().contains(kw)
                    || tx.getItemName().toLowerCase().contains(kw)
                    || tx.getWinnerId().toLowerCase().contains(kw)
                    || tx.getStatus().toLowerCase().contains(kw);
            boolean matchStatus = (status == null || status.equals(ALL_STATUS))
                    || tx.getStatus().equalsIgnoreCase(status);
            return matchKw && matchStatus;
        });
        updateCountLabel();
    }

    private void updateCountLabel() {
        int shown = filteredList.size();
        int total = masterList.size();
        lblTxCount.setText(shown == total
                ? total + " phiên"
                : shown + " / " + total + " phiên");
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── Model dữ liệu ─────────────────────────────────────────

    public static class Transaction {
        private final SimpleStringProperty auctionId;
        private final SimpleStringProperty productId;
        private final SimpleStringProperty itemName;
        private final SimpleStringProperty startTime;
        private final SimpleStringProperty endTime;
        private final SimpleStringProperty status;
        private final SimpleStringProperty winnerId;
        private final SimpleStringProperty finalPrice;

        public Transaction(String auctionId, String productId, String itemName, String startTime, String endTime,
                           String status, String winnerId, String finalPrice) {
            this.auctionId = new SimpleStringProperty(auctionId);
            this.productId  = new SimpleStringProperty(productId);
            this.itemName = new SimpleStringProperty(itemName);
            this.startTime  = new SimpleStringProperty(startTime);
            this.endTime    = new SimpleStringProperty(endTime);
            this.status     = new SimpleStringProperty(status);
            this.winnerId   = new SimpleStringProperty(winnerId);
            this.finalPrice = new SimpleStringProperty(finalPrice);
        }

        public String getAuctionId() { return auctionId.get(); }
        public String getProductId()  { return productId.get();  }
        public String getItemName() { return itemName.get(); }
        public String getStartTime()  { return startTime.get();  }
        public String getEndTime()    { return endTime.get();    }
        public String getStatus()     { return status.get();     }
        public String getWinnerId()   { return winnerId.get();   }
        public String getFinalPrice() { return finalPrice.get(); }

        // Trạng thái cần setter vì admin có thể thay đổi
        public void setStatus(String v) { status.set(v); }

        public SimpleStringProperty auctionIdProperty() { return auctionId; }
        public SimpleStringProperty productIdProperty()  { return productId;  }
        public SimpleStringProperty itemNameProperty() { return itemName; }
        public SimpleStringProperty startTimeProperty()  { return startTime;  }
        public SimpleStringProperty endTimeProperty()    { return endTime;    }
        public SimpleStringProperty statusProperty()     { return status;     }
        public SimpleStringProperty winnerIdProperty()   { return winnerId;   }
        public SimpleStringProperty finalPriceProperty() { return finalPrice; }
    }
}
