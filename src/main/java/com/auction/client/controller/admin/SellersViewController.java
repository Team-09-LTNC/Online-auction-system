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
import java.util.ResourceBundle;

import com.auction.client.manager.AdminManager;


/**
 * SellersViewController
 * ─────────────────────────────────────────────────────────────
 * Controller cho SellersView.fxml.
 * Danh sách người bán — admin có thể xoá hoặc khoá/mở khoá.
 */
public class SellersViewController implements Initializable {

    // ── FXML injections ──────────────────────────────────────
    @FXML private StackPane                   contentPane;
    @FXML private TableView<Seller>           sellerTable;
    @FXML private TableColumn<Seller, String> colUsername;
    @FXML private TableColumn<Seller, String> colFullname;
    @FXML private TableColumn<Seller, String> colStatus;
    @FXML private Label                       lblSellerCount;
    @FXML private TextField                   tfSearch;
    @FXML private ComboBox<String>            cbStatusFilter;
    @FXML private Button                      btnToggleLock;

    // ── Hằng số trạng thái ───────────────────────────────────
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_LOCKED = "LOCKED";

    private static final List<String> FILTER_OPTIONS =
            List.of("Tất cả", STATUS_ACTIVE, STATUS_LOCKED);

    // ── Data ─────────────────────────────────────────────────
    private final ObservableList<Seller> masterList   = FXCollections.observableArrayList();
    private       FilteredList<Seller>  filteredList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        setupStatusFilter();
        setupTable();
        loadData();
    }

    // ── Setup ────────────────────────────────────────────────

    private void setupColumns() {
        colUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colFullname.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullname()));

        // Cột trạng thái: badge màu ACTIVE (xanh) / LOCKED (đỏ)
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                boolean isLocked = STATUS_LOCKED.equals(status);
                setText(isLocked ? "🔒  Bị khoá" : "✅  Hoạt động");
                setStyle(isLocked
                        ? "-fx-text-fill: #C0392B; -fx-font-weight: bold; -fx-font-size: 12px;"
                        : "-fx-text-fill: #27AE60; -fx-font-weight: bold; -fx-font-size: 12px;");
            }
        });
    }

    private void setupStatusFilter() {
        cbStatusFilter.setItems(FXCollections.observableArrayList(FILTER_OPTIONS));
        cbStatusFilter.getSelectionModel().selectFirst();
    }

    private void setupTable() {
        filteredList = new FilteredList<>(masterList, p -> true);
        sellerTable.setItems(filteredList);
        sellerTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void loadData() {
    AdminManager.getInstance().layDanhSachSeller(
        users -> Platform.runLater(() -> {
            masterList.clear();
            users.forEach(u -> masterList.add(
                new Seller(u.getUsername(), u.getFullname(), u.getStatus())
            ));
            updateCountLabel();
        }),
        error -> Platform.runLater(() ->
            new Alert(Alert.AlertType.ERROR, error).showAndWait()
        )
    );
}

    // ── FXML handlers ────────────────────────────────────────

    @FXML
    private void handleTableClick(MouseEvent e) {
        Seller selected = sellerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            btnToggleLock.setDisable(true);
            return;
        }
        btnToggleLock.setDisable(false);
        refreshLockButtonText(selected);
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
     * Toggle khoá / mở khoá tài khoản người bán.
     * ACTIVE → LOCKED  hoặc  LOCKED → ACTIVE.
     */
    @FXML
    private void handleToggleLock() {
    Seller selected = sellerTable.getSelectionModel().getSelectedItem();
    if (selected == null) return;

    String newStatus = STATUS_LOCKED.equals(selected.getStatus()) ? STATUS_ACTIVE : STATUS_LOCKED;

    // ... dialog xác nhận giữ nguyên ...

    AdminManager.getInstance().toggleKhoaTaiKhoan(selected.getUsername(), newStatus,
        response -> Platform.runLater(() -> {
            selected.setStatus(newStatus);
            sellerTable.refresh();
            refreshLockButtonText(selected);
            applyFilter();
            updateCountLabel();
        }),
        error -> Platform.runLater(() ->
            new Alert(Alert.AlertType.ERROR, error).showAndWait())
    );
}


    // ── Helpers ──────────────────────────────────────────────

    private void applyFilter() {
        String kw     = tfSearch.getText().trim().toLowerCase();
        String status = cbStatusFilter.getValue();

        filteredList.setPredicate(s -> {
            boolean matchKw = kw.isEmpty()
                    || s.getUsername().toLowerCase().contains(kw)
                    || s.getFullname().toLowerCase().contains(kw);
            boolean matchStatus = (status == null || status.equals("Tất cả"))
                    || s.getStatus().equalsIgnoreCase(status);
            return matchKw && matchStatus;
        });
        updateCountLabel();
    }

    private void refreshLockButtonText(Seller seller) {
        if (STATUS_LOCKED.equals(seller.getStatus())) {
            btnToggleLock.setText("🔓  Mở khoá");
            btnToggleLock.setStyle(
                    "-fx-background-color: #27AE60; -fx-text-fill: white; "
                            + "-fx-font-size: 13px; -fx-font-weight: bold; "
                            + "-fx-padding: 8 18 8 18; -fx-border-radius: 6px; "
                            + "-fx-background-radius: 6px; -fx-cursor: hand; -fx-border-width: 0;");
        } else {
            btnToggleLock.setText("🔒  Khoá tài khoản");
            btnToggleLock.setStyle(
                    "-fx-background-color: #8B2C2C; -fx-text-fill: white; "
                            + "-fx-font-size: 13px; -fx-font-weight: bold; "
                            + "-fx-padding: 8 18 8 18; -fx-border-radius: 6px; "
                            + "-fx-background-radius: 6px; -fx-cursor: hand; -fx-border-width: 0;");
        }
    }

    // private void resetButtons() {
    //     btnToggleLock.setDisable(true);
    //     btnToggleLock.setText("🔒  Khoá tài khoản");
    //     btnToggleLock.setStyle(
    //         "-fx-background-color: #8B2C2C; -fx-text-fill: white; "
    //         + "-fx-font-size: 13px; -fx-font-weight: bold; "
    //         + "-fx-padding: 8 18 8 18; -fx-border-radius: 6px; "
    //         + "-fx-background-radius: 6px; -fx-cursor: hand; -fx-border-width: 0;");
    //     btnDelete.setDisable(true);
    //     sellerTable.getSelectionModel().clearSelection();
    // }

    private void updateCountLabel() {
        int shown = filteredList.size();
        int total = masterList.size();
        lblSellerCount.setText(shown == total
                ? total + " người bán"
                : shown + " / " + total + " người bán");
    }

    // ── Model ────────────────────────────────────────────────

    public static class Seller {
        private final SimpleStringProperty username;
        private final SimpleStringProperty fullname;
        private final SimpleStringProperty status;

        public Seller(String username, String fullname, String status) {
            this.username = new SimpleStringProperty(username);
            this.fullname = new SimpleStringProperty(fullname);
            this.status   = new SimpleStringProperty(status);
        }

        public String getUsername() { return username.get(); }
        public String getFullname() { return fullname.get(); }
        public String getStatus()   { return status.get();   }
        public void   setStatus(String v) { status.set(v);   }

        public SimpleStringProperty usernameProperty() { return username; }
        public SimpleStringProperty fullnameProperty() { return fullname; }
        public SimpleStringProperty statusProperty()   { return status;   }
    }
}
