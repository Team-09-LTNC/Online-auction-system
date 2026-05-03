package com.auction.client.controller;

import com.auction.common.model.user.User;
import com.auction.server.dao.ItemDao;
import com.auction.server.manager.ItemFormManager;
import com.auction.server.manager.ItemListManager;
import com.auction.server.manager.ItemRow;
import com.auction.server.utils.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;

public class SellerDashboardController {

    @FXML private Label lblWelcome;
    @FXML private Label lblStatus;

    @FXML private Button btnMenuMyItems;
    @FXML private Button btnMenuAddNew;

    @FXML private javafx.scene.layout.VBox paneMyItems;
    @FXML private ScrollPane paneAddNew;

    @FXML private TableView<ItemRow> itemTable;
    @FXML private TableColumn<ItemRow, String> colName;
    @FXML private TableColumn<ItemRow, String> colCategory;
    @FXML private TableColumn<ItemRow, String> colPrice;
    @FXML private TableColumn<ItemRow, String> colStatus;
    @FXML private TableColumn<ItemRow, Void> colAction;

    @FXML private ComboBox<String> cbCategory;
    @FXML private TextField tfName;
    @FXML private TextArea taDescription;
    @FXML private TextField tfPrice;
    @FXML private TextField tfDuration;

    @FXML private javafx.scene.layout.VBox paneArtFields;
    @FXML private javafx.scene.layout.VBox paneElecFields;
    @FXML private javafx.scene.layout.VBox paneVehicleFields;

    @FXML private TextField tfArtist;
    @FXML private TextField tfYear;
    @FXML private TextField tfMedium;

    @FXML private TextField tfBrand;
    @FXML private TextField tfWarranty;

    @FXML private TextField tfMake;
    @FXML private TextField tfModel;
    @FXML private TextField tfVehicleYear;

    @FXML private Button btnDang;
    @FXML private Button btnLogout;

    private User currentSeller;
    private ItemDao itemDao;
    private ObservableList<ItemRow> danhSachSanPham;

    private ItemListManager itemListManager;
    private ItemFormManager itemFormManager;

    @FXML
    public void initialize() {
        java.sql.Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            this.itemDao = new ItemDao(conn);
            System.out.println(">>> [SUCCESS] SellerDashboard đã kết nối với Database Cloud.");
        } else {
            showError("Lỗi: Không thể kết nối tới Database. Vui lòng kiểm tra Internet hoặc IP Whitelist!");
        }

        cbCategory.getItems().addAll("ART", "ELECTRONICS", "VEHICLE");

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        setupCotHanhDong();

        danhSachSanPham = FXCollections.observableArrayList();
        itemTable.setItems(danhSachSanPham);

        hienPaneMyItems();
    }

    public void initSeller(User seller) {
        this.currentSeller = seller;
        lblWelcome.setText("Xin chào, " + seller.getFullName() + "!");

        // Initialize managers
        this.itemListManager = new ItemListManager(itemDao, currentSeller, danhSachSanPham);
        this.itemFormManager = new ItemFormManager(itemDao, currentSeller);

        // Setup form controls
        itemFormManager.setFormControls(cbCategory, tfName, taDescription, tfPrice, tfDuration,
                tfArtist, tfYear, tfMedium, tfBrand, tfWarranty, tfMake, tfModel, tfVehicleYear,
                btnDang, paneArtFields, paneElecFields, paneVehicleFields, lblStatus);

        // Setup callbacks
        itemFormManager.setOnItemSaved(() -> itemListManager.loadDanhSach());
        itemFormManager.setOnEditMode(() -> hienPaneAddNew());

        if (itemDao != null) {
            itemListManager.loadDanhSach();
        }
    }

    @FXML
    private void onMenuMyItemsClick() {
        hienPaneMyItems();
        if (itemListManager != null) {
            itemListManager.loadDanhSach();
        }
    }

    @FXML
    private void onMenuAddNewClick() {
        hienPaneAddNew();
        if (itemFormManager != null) {
            itemFormManager.clearForm();
            itemFormManager.setDangSuaId(-1);
        }
    }

    private void hienPaneMyItems() {
        paneMyItems.setVisible(true);
        paneMyItems.setManaged(true);
        paneAddNew.setVisible(false);
        paneAddNew.setManaged(false);
        btnMenuMyItems.setStyle("-fx-background-color: #6E1C1C; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 12 15 12 15; -fx-background-radius: 0; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;");
        btnMenuAddNew.setStyle("-fx-background-color: transparent; -fx-text-fill: #1A0F0A; -fx-font-size: 13px; -fx-padding: 12 15 12 15; -fx-background-radius: 0; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;");
    }

    private void hienPaneAddNew() {
        paneAddNew.setVisible(true);
        paneAddNew.setManaged(true);
        paneMyItems.setVisible(false);
        paneMyItems.setManaged(false);
        btnMenuAddNew.setStyle("-fx-background-color: #6E1C1C; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 12 15 12 15; -fx-background-radius: 0; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;");
        btnMenuMyItems.setStyle("-fx-background-color: transparent; -fx-text-fill: #1A0F0A; -fx-font-size: 13px; -fx-padding: 12 15 12 15; -fx-background-radius: 0; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;");
    }

    private void setupCotHanhDong() {
        colAction.setCellFactory(col -> new TableCell<>() {
            Button btnSua = new Button("Sửa");
            Button btnXoa = new Button("Xóa");
            HBox box = new HBox(5, btnSua, btnXoa);

            {
                btnSua.setStyle("-fx-background-color: #6E1C1C; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
                btnXoa.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");

                btnSua.setOnAction(e -> {
                    ItemRow row = getTableView().getItems().get(getIndex());
                    if (itemFormManager != null) {
                        itemFormManager.chuyenSangSuaSanPham(row);
                    }
                });

                btnXoa.setOnAction(e -> {
                    ItemRow row = getTableView().getItems().get(getIndex());
                    if (itemListManager != null) {
                        itemListManager.xoaSanPham(row);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ItemRow row = getTableView().getItems().get(getIndex());
                    if ("OPEN".equals(row.getStatus())) {
                        setGraphic(box);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    @FXML
    private void onCategoryChanged() {
        if (itemFormManager != null) {
            itemFormManager.onCategoryChanged();
        }
    }

    @FXML
    private void onDangClick() {
        if (itemFormManager != null) {
            itemFormManager.onDangClick();
        }
    }

    @FXML
    private void onClearFormClick() {
        if (itemFormManager != null) {
            itemFormManager.resetForm();
        }
    }

    @FXML
    private void onRefreshClick() {
        if (itemListManager != null) {
            itemListManager.loadDanhSach();
        }
        System.out.println("Da tai lai danh sach");
    }

    @FXML
    private void onLogoutClick() {
        if (itemDao != null) {
            itemDao.closeConnection();
            itemDao = null;
        }
        try {
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
            stage.setTitle("HỆ THỐNG ĐẤU GIÁ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
    }
}
