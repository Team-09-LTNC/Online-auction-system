package com.auction.client.controller.admin;

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
 * TransactionsViewController
 * ─────────────────────────────────────────────────────────────
 * Controller cho TransactionsView.fxml.
 * Phiên đấu giá đã kết thúc; admin có thể thay đổi status.
 */
public class TransactionsViewController implements Initializable {

    // ── FXML injections ──────────────────────────────────────
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

    // ── Data ─────────────────────────────────────────────────
    private final ObservableList<Transaction> masterList   = FXCollections.observableArrayList();
    private       FilteredList<Transaction>  filteredList;

    /** Các trạng thái có thể chuyển sang khi admin chỉnh sửa. */
    private static final List<String> EDITABLE_STATUSES =
            List.of("COMPLETED", "CANCELLED", "DISPUTED", "PENDING_PAYMENT");

    /** Tuỳ chọn lọc trên ComboBox. */
    private static final List<String> FILTER_OPTIONS =
            List.of("Tất cả", "COMPLETED", "CANCELLED", "DISPUTED", "PENDING_PAYMENT");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        setupStatusFilter();
        setupTable();
        loadData();
    }

    // ── Setup ────────────────────────────────────────────────

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
        masterList.setAll(
                new Transaction("SP-001", "2025-05-01 09:00", "2025-05-01 12:00",
                        "COMPLETED",      "user_88",  "15.500.000"),
                new Transaction("SP-002", "2025-05-02 14:00", "2025-05-02 18:00",
                        "CANCELLED",      "",         "0"),
                new Transaction("SP-003", "2025-05-03 08:00", "2025-05-03 10:00",
                        "DISPUTED",       "user_44",  "8.200.000"),
                new Transaction("SP-004", "2025-05-04 10:00", "2025-05-04 14:00",
                        "PENDING_PAYMENT","user_12",  "22.000.000")
        );
        updateCountLabel();
    }

    // ── FXML handlers ────────────────────────────────────────

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

        // Tạo ChoiceDialog để admin chọn status mới
        ChoiceDialog<String> dialog = new ChoiceDialog<>(selected.getStatus(), EDITABLE_STATUSES);
        dialog.setTitle("Thay đổi trạng thái");
        dialog.setHeaderText("Phiên: " + selected.getProductId());
        dialog.setContentText("Chọn trạng thái mới:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newStatus -> {
            if (!newStatus.equals(selected.getStatus())) {
                selected.setStatus(newStatus);           // cập nhật model
                txTable.refresh();                       // refresh cell
                showInfo("Cập nhật thành công",
                        "Trạng thái phiên " + selected.getProductId()
                                + " đã được đổi thành: " + newStatus);
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────

    private void applyFilter() {
        String kw     = tfSearch.getText().trim().toLowerCase();
        String status = cbStatusFilter.getValue();

        filteredList.setPredicate(tx -> {
            boolean matchKw = kw.isEmpty()
                    || tx.getProductId().toLowerCase().contains(kw)
                    || tx.getWinnerId().toLowerCase().contains(kw)
                    || tx.getStatus().toLowerCase().contains(kw);
            boolean matchStatus = (status == null || status.equals("Tất cả"))
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

    // ── Model ────────────────────────────────────────────────

    public static class Transaction {
        private final SimpleStringProperty productId;
        private final SimpleStringProperty startTime;
        private final SimpleStringProperty endTime;
        private final SimpleStringProperty status;
        private final SimpleStringProperty winnerId;
        private final SimpleStringProperty finalPrice;

        public Transaction(String productId, String startTime, String endTime,
                           String status, String winnerId, String finalPrice) {
            this.productId  = new SimpleStringProperty(productId);
            this.startTime  = new SimpleStringProperty(startTime);
            this.endTime    = new SimpleStringProperty(endTime);
            this.status     = new SimpleStringProperty(status);
            this.winnerId   = new SimpleStringProperty(winnerId);
            this.finalPrice = new SimpleStringProperty(finalPrice);
        }

        public String getProductId()  { return productId.get();  }
        public String getStartTime()  { return startTime.get();  }
        public String getEndTime()    { return endTime.get();    }
        public String getStatus()     { return status.get();     }
        public String getWinnerId()   { return winnerId.get();   }
        public String getFinalPrice() { return finalPrice.get(); }

        // status cần setter vì admin có thể thay đổi
        public void setStatus(String v) { status.set(v); }

        public SimpleStringProperty productIdProperty()  { return productId;  }
        public SimpleStringProperty startTimeProperty()  { return startTime;  }
        public SimpleStringProperty endTimeProperty()    { return endTime;    }
        public SimpleStringProperty statusProperty()     { return status;     }
        public SimpleStringProperty winnerIdProperty()   { return winnerId;   }
        public SimpleStringProperty finalPriceProperty() { return finalPrice; }
    }
}
