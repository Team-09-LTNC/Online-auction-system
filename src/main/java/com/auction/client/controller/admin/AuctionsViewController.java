package com.auction.client.controller.admin;

import com.auction.client.manager.AdminManager;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**
 * Controller for auctions administration view.
 */
public class AuctionsViewController implements Initializable {

    private static final String ALL_STATUS = "Tat ca";
    private static final List<String> STATUS_OPTIONS = List.of(
            ALL_STATUS,
            "OPEN",
            "RUNNING",
            "FINISHED",
            "PAID",
            "CANCELED");

    @FXML
    private StackPane contentPane;
    @FXML
    private TableView<Auction> auctionTable;
    @FXML
    private TableColumn<Auction, String> colProductId;
    @FXML
    private TableColumn<Auction, String> colStartTime;
    @FXML
    private TableColumn<Auction, String> colEndTime;
    @FXML
    private TableColumn<Auction, String> colStatus;
    @FXML
    private Label lblAuctionCount;
    @FXML
    private TextField tfSearch;
    @FXML
    private ComboBox<String> cbStatusFilter;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnChangeStatus; // ← THÊM

    private final ObservableList<Auction> masterList = FXCollections.observableArrayList();
    private FilteredList<Auction> filteredList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        setupStatusFilter();
        setupTable();
        loadData();
    }

    private void setupColumns() {
        colProductId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductId()));
        colStartTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStartTime()));
        colEndTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEndTime()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
    }

    private void setupStatusFilter() {
        cbStatusFilter.setItems(FXCollections.observableArrayList(STATUS_OPTIONS));
        cbStatusFilter.getSelectionModel().selectFirst();
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
                    auctions.forEach(a -> masterList.add(new Auction(
                            String.valueOf(a.getId()),
                            a.getItemName(),
                            a.getStartTime(),
                            a.getEndTime(),
                            a.getStatus(),
                            a.getImageUrl())));
                    updateCountLabel();
                }),
                error -> Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, error).showAndWait()));
    }

    @FXML
    private void handleTableClick(MouseEvent event) {
        boolean selected = auctionTable.getSelectionModel().getSelectedItem() != null;
        btnDelete.setDisable(!selected);
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

    @FXML
    private void handleDelete() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xac nhan xoa");
        confirm.setHeaderText("Xoa phien dau gia? Toàn bộ lich sử đấu giá sẽ bị xóa và không thể khôi phục.");
        confirm.setContentText("Xoa phien cua san pham \"" + selected.getProductId() + "\"?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            masterList.remove(selected);
            btnDelete.setDisable(true);
            updateCountLabel();
        }
    }

    private void applyFilter() {
        String kw = tfSearch.getText().trim().toLowerCase();
        String status = cbStatusFilter.getValue();

        filteredList.setPredicate(a -> {
            boolean matchKw = kw.isEmpty()
                    || a.getProductId().toLowerCase().contains(kw)
                    || a.getStatus().toLowerCase().contains(kw);
            boolean matchStatus = status == null || ALL_STATUS.equals(status)
                    || a.getStatus().equalsIgnoreCase(status);
            return matchKw && matchStatus;
        });
        updateCountLabel();
    }

    private void updateCountLabel() {
        int shown = filteredList.size();
        int total = masterList.size();
        lblAuctionCount.setText(shown == total
                ? total + " phien"
                : shown + " / " + total + " phien");
    }

    @FXML
    private void handleChangeStatus() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        String currentStatus = selected.getStatus();

        // Xác định các lựa chọn hợp lệ theo status hiện tại
        List<String> choices;
        switch (currentStatus) {
            case "OPEN":
            case "RUNNING":
                choices = List.of("CANCELED");
                break;
            case "CANCELED":
                choices = List.of("REOPEN"); // server tự tính OPEN/RUNNING/FINISHED
                break;
            case "FINISHED":
                choices = List.of("PAID");
                break;
            default:
                new Alert(Alert.AlertType.WARNING,
                        "Không thể thay đổi trạng thái phiên đang " + currentStatus).showAndWait();
                return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle("Thay đổi trạng thái");
        dialog.setHeaderText(null);
        dialog.setContentText("Phiên \"" + selected.getProductId() +
                "\" đang " + currentStatus + "\nChọn hành động:");

        dialog.showAndWait().ifPresent(action -> {
            AdminManager.getInstance().thayDoiTrangThaiAuction(
                    Integer.parseInt(selected.getAuctionId()),
                    action,
                    msg -> Platform.runLater(() -> {
                        // Lấy status thực từ response nếu là REOPEN
                        String displayStatus = action.equals("REOPEN")
                                ? msg.replace("Đã cập nhật thành ", "").replace("!", "")
                                : action;
                        selected.setStatus(displayStatus);
                        auctionTable.refresh();
                        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
                    }),
                    error -> Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, error).showAndWait()));
        });
    }

    /**
     * Table item model.
     */
    public static class Auction {
        private final SimpleStringProperty auctionId; // ← THÊM
        private final SimpleStringProperty productId;
        private final SimpleStringProperty startTime;
        private final SimpleStringProperty endTime;
        private final SimpleStringProperty status;
        private final SimpleStringProperty imageUrl;

        public Auction(String auctionId, String productId, String startTime,
                String endTime, String status, String imageUrl) {
            this.auctionId = new SimpleStringProperty(auctionId); // ← THÊM
            this.productId = new SimpleStringProperty(productId);
            this.startTime = new SimpleStringProperty(startTime);
            this.endTime = new SimpleStringProperty(endTime);
            this.status = new SimpleStringProperty(status);
            this.imageUrl = new SimpleStringProperty(imageUrl);
        }

        public String getAuctionId() {
            return auctionId.get();
        } // ← THÊM

        public String getProductId() {
            return productId.get();
        }

        public String getStartTime() {
            return startTime.get();
        }

        public String getEndTime() {
            return endTime.get();
        }

        public String getStatus() {
            return status.get();
        }

        public String getImageUrl() {
            return imageUrl.get();
        }

        public void setStatus(String s) {
            status.set(s);
        } // ← THÊM để cập nhật UI
    }
}
