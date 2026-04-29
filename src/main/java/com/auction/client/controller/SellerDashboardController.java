package com.auction.client.controller;

import com.auction.auction.AuctionStatus;
import com.auction.common.model.item.*;
import com.auction.common.model.user.User;
import com.auction.server.dao.ItemDao;
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
import java.sql.SQLException;
import java.util.List;

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
    private int dangSuaId = -1;

    @FXML
    public void initialize() {
        java.sql.Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            this.itemDao = new ItemDao(conn);
            System.out.println(">>> [SUCCESS] SellerDashboard đã kết nối với Database Cloud.");
        } else {
            hienThiLoi("Lỗi: Không thể kết nối tới Database. Vui lòng kiểm tra Internet hoặc IP Whitelist!");
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

        // FIX BUG 4 (phần 1): Xóa lệnh gọi loadDanhSach() ở đây.
        // initSeller() sẽ được gọi ngay sau từ LoginController và đã gọi loadDanhSach() rồi.
        // Gọi ở đây vừa thừa (tải 2 lần) vừa sai vì currentSeller chưa được gán.
    }

    public void initSeller(User seller) {
        this.currentSeller = seller;
        lblWelcome.setText("Xin chào, " + seller.getFullName() + "!");
        // FIX BUG 4 (phần 2): Thêm null check trước khi gọi loadDanhSach()
        if (itemDao != null) {
            loadDanhSach();
        }
    }

    @FXML
    private void onMenuMyItemsClick() {
        hienPaneMyItems();
        // FIX BUG 4 (phần 2): Thêm null check
        if (itemDao != null) {
            loadDanhSach();
        }
    }

    @FXML
    private void onMenuAddNewClick() {
        hienPaneAddNew();
        xoaForm();
        dangSuaId = -1;
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

    private void loadDanhSach() {
        // FIX BUG 4 (guard clause): Bảo vệ khỏi NullPointerException nếu itemDao null
        if (itemDao == null) {
            hienThiLoi("Lỗi: Không có kết nối Database!");
            return;
        }

        danhSachSanPham.clear();
        List<ItemDao.ItemRecord> records = itemDao.findAll();

        for (ItemDao.ItemRecord record : records) {
            danhSachSanPham.add(new ItemRow(
                    record.dbId,
                    record.item.getName(),
                    record.item.getItemCategory(),
                    String.valueOf(record.item.getStartingPrice()),
                    record.status.name()
            ));
        }
        System.out.println("Da load " + danhSachSanPham.size() + " san pham");
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
                    chuyenSangSuaSanPham(row);
                });

                btnXoa.setOnAction(e -> {
                    ItemRow row = getTableView().getItems().get(getIndex());
                    xoaSanPham(row);
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

    private void chuyenSangSuaSanPham(ItemRow row) {
        dangSuaId = row.getDbId();
        ItemDao.ItemRecord record = itemDao.findByDbId(dangSuaId);
        if (record == null) {
            System.out.println("Khong tim thay san pham id = " + dangSuaId);
            return;
        }

        cbCategory.setValue(record.item.getItemCategory());
        tfName.setText(record.item.getName());
        taDescription.setText(record.item.getDescription());
        tfPrice.setText(String.valueOf(record.item.getStartingPrice()));

        if (record.item instanceof Art art) {
            tfArtist.setText(art.getArtist());
            tfYear.setText(String.valueOf(art.getYearCreated()));
            tfMedium.setText(art.getMedium());
        } else if (record.item instanceof Electronics elec) {
            tfBrand.setText(elec.getBrand());
            tfWarranty.setText(String.valueOf(elec.getWarrantyMonths()));
        } else if (record.item instanceof Vehicle vehicle) {
            tfMake.setText(vehicle.getMake());
            tfModel.setText(vehicle.getModel());
            tfVehicleYear.setText(String.valueOf(vehicle.getYear()));
        }

        hienPaneAddNew();
        btnDang.setText("Cập nhật sản phẩm");
        hienThiThongBao("Đang sửa sản phẩm: " + record.item.getName());
    }

    private void xoaSanPham(ItemRow row) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc muốn xóa sản phẩm \"" + row.getName() + "\" không?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            boolean ketQua = itemDao.deleteById(row.getDbId());
            if (ketQua) {
                loadDanhSach();
                System.out.println("Da xoa san pham id = " + row.getDbId());
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Lỗi");
                err.setHeaderText(null);
                err.setContentText("Không thể xóa sản phẩm này!");
                err.showAndWait();
            }
        }
    }

    @FXML
    private void onCategoryChanged() {
        String loai = cbCategory.getValue();

        paneArtFields.setVisible(false);
        paneArtFields.setManaged(false);
        paneElecFields.setVisible(false);
        paneElecFields.setManaged(false);
        paneVehicleFields.setVisible(false);
        paneVehicleFields.setManaged(false);

        if (loai == null) return;

        switch (loai) {
            case "ART" -> {
                paneArtFields.setVisible(true);
                paneArtFields.setManaged(true);
            }
            case "ELECTRONICS" -> {
                paneElecFields.setVisible(true);
                paneElecFields.setManaged(true);
            }
            case "VEHICLE" -> {
                paneVehicleFields.setVisible(true);
                paneVehicleFields.setManaged(true);
            }
        }
    }

    @FXML
    private void onDangClick() {
        String loai = cbCategory.getValue();
        String ten = tfName.getText().trim();
        String moTa = taDescription.getText().trim();
        String giaText = tfPrice.getText().trim();

        if (loai == null) { hienThiLoi("Lỗi: Chưa chọn loại sản phẩm!"); return; }
        if (ten.isEmpty()) { hienThiLoi("Lỗi: Chưa nhập tên sản phẩm!"); return; }
        if (giaText.isEmpty()) { hienThiLoi("Lỗi: Chưa nhập giá khởi điểm!"); return; }

        double gia;
        try {
            gia = Double.parseDouble(giaText);
            if (gia <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            hienThiLoi("Lỗi: Giá khởi điểm không hợp lệ!");
            return;
        }

        if (dangSuaId == -1) {
            themSanPhamMoi(loai, ten, moTa, gia);
        } else {
            capNhatSanPham(ten, moTa, gia);
        }
    }

    private void themSanPhamMoi(String loai, String ten, String moTa, double gia) {
        if (currentSeller == null) {
            hienThiLoi("❌ Lỗi: Không tìm thấy thông tin người bán. Vui lòng đăng nhập lại!");
            return;
        }

        Item sanPham;
        switch (loai) {
            case "ART" -> {
                String artist = tfArtist.getText().trim();
                int year = 0;
                try { year = Integer.parseInt(tfYear.getText().trim()); } catch (Exception ignored) {}
                String medium = tfMedium.getText().trim();
                sanPham = new Art(ten, moTa, gia, artist, year, medium);
            }
            case "ELECTRONICS" -> {
                String brand = tfBrand.getText().trim();
                int warranty = 0;
                try { warranty = Integer.parseInt(tfWarranty.getText().trim()); } catch (Exception ignored) {}
                sanPham = new Electronics(ten, moTa, gia, brand, warranty);
            }
            case "VEHICLE" -> {
                String make = tfMake.getText().trim();
                String model = tfModel.getText().trim();
                int vehicleYear = 0;
                try { vehicleYear = Integer.parseInt(tfVehicleYear.getText().trim()); } catch (Exception ignored) {}
                sanPham = new Vehicle(ten, moTa, gia, make, model, vehicleYear);
            }
            default -> {
                hienThiLoi("Loại sản phẩm không hợp lệ!");
                return;
            }
        }

        int dbId = itemDao.saveItem(sanPham, currentSeller.getId());
        if (dbId > 0) {
            hienThiThanhCong("✅ Đăng sản phẩm thành công!");
            xoaForm();
            loadDanhSach();
        } else {
            hienThiLoi("❌ Lỗi: Không lưu được sản phẩm vào Database.");
        }
    }

    private void capNhatSanPham(String ten, String moTa, double gia) {
        boolean ketQua = itemDao.updateItemInfo(dangSuaId, ten, moTa, gia);
        if (ketQua) {
            hienThiThanhCong("✅ Cập nhật sản phẩm thành công!");
            xoaForm();
            dangSuaId = -1;
            btnDang.setText("Đăng sản phẩm");
        } else {
            hienThiLoi("❌ Không thể cập nhật! Có thể sản phẩm đang RUNNING.");
        }
    }

    @FXML
    private void onClearFormClick() {
        xoaForm();
        dangSuaId = -1;
        btnDang.setText("Đăng sản phẩm");
        lblStatus.setText("");
    }

    @FXML
    private void onRefreshClick() {
        // FIX BUG 4 (guard clause)
        if (itemDao != null) {
            loadDanhSach();
        }
        System.out.println("Da tai lai danh sach");
    }

    //  Đóng Connection trước khi thoát để tránh connection leak
    @FXML
    private void onLogoutClick() {
        // Đóng kết nối DB trước khi rời màn hình
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

    private void xoaForm() {
        cbCategory.setValue(null);
        tfName.clear();
        taDescription.clear();
        tfPrice.clear();
        tfDuration.clear();
        tfArtist.clear();
        tfYear.clear();
        tfMedium.clear();
        tfBrand.clear();
        tfWarranty.clear();
        tfMake.clear();
        tfModel.clear();
        tfVehicleYear.clear();
        paneArtFields.setVisible(false);
        paneArtFields.setManaged(false);
        paneElecFields.setVisible(false);
        paneElecFields.setManaged(false);
        paneVehicleFields.setVisible(false);
        paneVehicleFields.setManaged(false);
    }

    private void hienThiLoi(String msg) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
    }

    private void hienThiThanhCong(String msg) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
    }

    private void hienThiThongBao(String msg) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-text-fill: #6E1C1C; -fx-font-size: 12px;");
    }

    public static class ItemRow {
        private int dbId;
        private String name;
        private String category;
        private String startingPrice;
        private String status;

        public ItemRow(int dbId, String name, String category, String startingPrice, String status) {
            this.dbId = dbId;
            this.name = name;
            this.category = category;
            this.startingPrice = startingPrice;
            this.status = status;
        }

        public int getDbId() { return dbId; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public String getStartingPrice() { return startingPrice; }
        public String getStatus() { return status; }
    }
}
