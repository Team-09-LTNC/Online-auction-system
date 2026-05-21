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
 * BiddersViewController
 * ─────────────────────────────────────────────────────────────
 * Controller cho BiddersView.fxml.
 * Danh sách người đấu giá — admin có thể xoá hoặc khoá/mở khoá.
 */
public class BiddersViewController implements Initializable {

    // ── FXML injections ──────────────────────────────────────
    @FXML
    private StackPane contentPane;
    @FXML
    private TableView<Bidder> bidderTable;
    @FXML
    private TableColumn<Bidder, String> colUsername;
    @FXML
    private TableColumn<Bidder, String> colFullname;
    @FXML
    private TableColumn<Bidder, String> colStatus; // cột trạng thái mới
    @FXML
    private Label lblBidderCount;
    @FXML
    private TextField tfSearch;
    @FXML
    private ComboBox<String> cbStatusFilter;
    @FXML
    private Button btnToggleLock;

    // ── Hằng số trạng thái ───────────────────────────────────
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_LOCKED = "LOCKED";

    private static final List<String> FILTER_OPTIONS = List.of("Tất cả", STATUS_ACTIVE, STATUS_LOCKED);

    // ── Data ─────────────────────────────────────────────────
    private final ObservableList<Bidder> masterList = FXCollections.observableArrayList();
    private FilteredList<Bidder> filteredList;

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
        cbStatusFilter.getSelectionModel().selectFirst(); // "Tất cả"
    }

    private void setupTable() {
        filteredList = new FilteredList<>(masterList, p -> true);
        bidderTable.setItems(filteredList);
        bidderTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void loadData() {
        AdminManager.getInstance().layDanhSachBidder(
                users -> Platform.runLater(() -> {
                    masterList.clear();
                    users.forEach(u -> masterList.add(
                            new Bidder(u.getUsername(), u.getFullname(), u.getStatus())));
                    updateCountLabel();
                }),
                error -> Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, error).showAndWait()));
    }

    // ── FXML handlers ────────────────────────────────────────

    /** Cập nhật trạng thái nút khi chọn hàng. */
    @FXML
    private void handleTableClick(MouseEvent e) {
        Bidder selected = bidderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            btnToggleLock.setDisable(true);
            return;
        }
        btnToggleLock.setDisable(false);
        // Đổi text nút tuỳ trạng thái hàng đang chọn
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
     * Toggle khoá / mở khoá tài khoản người đấu giá.
     * Nếu đang ACTIVE → chuyển sang LOCKED.
     * Nếu đang LOCKED → chuyển sang ACTIVE.
     */
    @FXML
    private void handleToggleLock() {
        Bidder selected = bidderTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        String newStatus = STATUS_LOCKED.equals(selected.getStatus()) ? STATUS_ACTIVE : STATUS_LOCKED;

        // ... dialog xác nhận giữ nguyên ...

        AdminManager.getInstance().toggleKhoaTaiKhoan(selected.getUsername(), newStatus,
                response -> Platform.runLater(() -> {
                    selected.setStatus(newStatus);
                    bidderTable.refresh();
                    refreshLockButtonText(selected);
                    applyFilter();
                    updateCountLabel();
                }),
                error -> Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, error).showAndWait()));
    }

    // ── Helpers ──────────────────────────────────────────────

    private void applyFilter() {
        String kw = tfSearch.getText().trim().toLowerCase();
        String status = cbStatusFilter.getValue();

        filteredList.setPredicate(b -> {
            boolean matchKw = kw.isEmpty()
                    || b.getUsername().toLowerCase().contains(kw)
                    || b.getFullname().toLowerCase().contains(kw);
            boolean matchStatus = (status == null || status.equals("Tất cả"))
                    || b.getStatus().equalsIgnoreCase(status);
            return matchKw && matchStatus;
        });
        updateCountLabel();
    }

    /** Đổi text + màu nền nút khoá tuỳ trạng thái của hàng được chọn. */
    private void refreshLockButtonText(Bidder bidder) {
        if (STATUS_LOCKED.equals(bidder.getStatus())) {
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
    // btnToggleLock.setDisable(true);
    // btnToggleLock.setText("🔒 Khoá tài khoản");
    // btnToggleLock.setStyle(
    // "-fx-background-color: #8B2C2C; -fx-text-fill: white; "
    // + "-fx-font-size: 13px; -fx-font-weight: bold; "
    // + "-fx-padding: 8 18 8 18; -fx-border-radius: 6px; "
    // + "-fx-background-radius: 6px; -fx-cursor: hand; -fx-border-width: 0;");
    // bidderTable.getSelectionModel().clearSelection();
    // }

    private void updateCountLabel() {
        int shown = filteredList.size();
        int total = masterList.size();
        lblBidderCount.setText(shown == total
                ? total + " người dùng"
                : shown + " / " + total + " người dùng");
    }

    // ── Model ────────────────────────────────────────────────

    public static class Bidder {
        private final SimpleStringProperty username;
        private final SimpleStringProperty fullname;
        private final SimpleStringProperty status; // "ACTIVE" | "LOCKED"

        public Bidder(String username, String fullname, String status) {
            this.username = new SimpleStringProperty(username);
            this.fullname = new SimpleStringProperty(fullname);
            this.status = new SimpleStringProperty(status);
        }

        public String getUsername() {
            return username.get();
        }

        public String getFullname() {
            return fullname.get();
        }

        public String getStatus() {
            return status.get();
        }

        public void setStatus(String v) {
            status.set(v);
        }

        public SimpleStringProperty usernameProperty() {
            return username;
        }

        public SimpleStringProperty fullnameProperty() {
            return fullname;
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }
    }
}
