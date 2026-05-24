package com.auction.client.controller.admin;

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

import com.auction.client.manager.AdminManager;

/**
 * AuctionsViewController
 * ─────────────────────────────────────────────────────────────
 * Controller cho AuctionsView.fxml.
 * Hiển thị phiên đấu giá đang & sắp diễn ra; admin có thể xoá.
 */
public class AuctionsViewController implements Initializable {

    // ── FXML injections ──────────────────────────────────────
    @FXML private StackPane                      contentPane;
    @FXML private TableView<Auction>             auctionTable;
    @FXML private TableColumn<Auction, String>   colProductId;
    @FXML private TableColumn<Auction, String>   colStartTime;
    @FXML private TableColumn<Auction, String>   colEndTime;
    @FXML private TableColumn<Auction, String>   colStatus;
    // @FXML private TableColumn<Auction, String>   colImageUrl;
    @FXML private Label                          lblAuctionCount;
    @FXML private TextField                      tfSearch;
    @FXML private ComboBox<String>               cbStatusFilter;
    @FXML private Button                         btnDelete;

    // ── Data ─────────────────────────────────────────────────
    private final ObservableList<Auction> masterList   = FXCollections.observableArrayList();
    private       FilteredList<Auction>  filteredList;

    /** Trạng thái hợp lệ của phiên đấu giá đang/sắp diễn ra. */
    private static final List<String> STATUS_OPTIONS =
            List.of("Tất cả", "UPCOMING", "ONGOING");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        setupStatusFilter();
        setupTable();
        loadData();
    }

    // ── Setup ────────────────────────────────────────────────

    private void setupColumns() {
        colProductId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductId()));
        colStartTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStartTime()));
        colEndTime  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEndTime()));
        colStatus   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        // colImageUrl .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getImageUrl()));
    }

    private void setupStatusFilter() {
        cbStatusFilter.setItems(FXCollections.observableArrayList(STATUS_OPTIONS));
        cbStatusFilter.getSelectionModel().selectFirst();   // "Tất cả"
    }

    private void setupTable() {
        filteredList = new FilteredList<>(masterList, p -> true);
        auctionTable.setItems(filteredList);
        auctionTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void loadData() {
        AdminManager.getInstance().layDanhSachAuction(
                auctions -> Platform.runLater(() -> {
                    masterList.clear();
                    auctions.forEach(a -> masterList.add(
                            new Auction(a.getItemName(), a.getStartTime(), a.getEndTime(), a.getStatus(), a.getImageUrl())));
                    updateCountLabel();
                }),
                error -> Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, error).showAndWait()));
    }

    // ── FXML handlers ────────────────────────────────────────

    @FXML
    private void handleTableClick(MouseEvent e) {
        boolean selected = auctionTable.getSelectionModel().getSelectedItem() != null;
        btnDelete.setDisable(!selected);
    }

    @FXML
    private void handleSearch() {
        applyFilter();
    }

    @FXML
    private void handleFilter() {
        applyFilter();
    }

    @FXML
    private void handleDelete() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xoá");
        confirm.setHeaderText("Xoá phiên đấu giá?");
        confirm.setContentText("Xoá phiên của sản phẩm \"" + selected.getProductId() + "\"?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            masterList.remove(selected);       // TODO: gọi service xoá trên server
            btnDelete.setDisable(true);
            updateCountLabel();
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private void applyFilter() {
        String kw     = tfSearch.getText().trim().toLowerCase();
        String status = cbStatusFilter.getValue();

        filteredList.setPredicate(a -> {
            boolean matchKw = kw.isEmpty()
                    || a.getProductId().toLowerCase().contains(kw)
                    || a.getStatus().toLowerCase().contains(kw);
            boolean matchStatus = (status == null || status.equals("Tất cả"))
                    || a.getStatus().equalsIgnoreCase(status);
            return matchKw && matchStatus;
        });
        updateCountLabel();
    }

    private void updateCountLabel() {
        int shown = filteredList.size();
        int total = masterList.size();
        lblAuctionCount.setText(shown == total
                ? total + " phiên"
                : shown + " / " + total + " phiên");
    }

    // ── Model ────────────────────────────────────────────────

    public static class Auction {
        private final SimpleStringProperty productId;
        private final SimpleStringProperty startTime;
        private final SimpleStringProperty endTime;
        private final SimpleStringProperty status;
        private final SimpleStringProperty imageUrl;

        public Auction(String productId, String startTime, String endTime, String status, String imageUrl) {
            this.productId = new SimpleStringProperty(productId);
            this.startTime = new SimpleStringProperty(startTime);
            this.endTime   = new SimpleStringProperty(endTime);
            this.status    = new SimpleStringProperty(status);
            this.imageUrl  = new SimpleStringProperty(imageUrl);
        }

        public String getProductId() { return productId.get(); }
        public String getStartTime() { return startTime.get(); }
        public String getEndTime()   { return endTime.get(); }
        public String getStatus()    { return status.get(); }
        public String getImageUrl()  { return imageUrl.get(); }

        public SimpleStringProperty productIdProperty() { return productId; }
        public SimpleStringProperty startTimeProperty() { return startTime; }
        public SimpleStringProperty endTimeProperty()   { return endTime;   }
        public SimpleStringProperty statusProperty()    { return status;    }
        public SimpleStringProperty imageUrlProperty()  { return imageUrl;  }
    }
}
