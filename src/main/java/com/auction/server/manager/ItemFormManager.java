package com.auction.server.manager;

import com.auction.common.model.item.*;
import com.auction.common.model.user.User;
import com.auction.server.dao.ItemDao;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class ItemFormManager {
    private ItemDao itemDao;
    private User currentSeller;
    private int dangSuaId = -1;

    // Form sản phẩm
    private ComboBox<String> cbCategory;
    private TextField tfName;
    private TextArea taDescription;
    private TextField tfPrice;
    private TextField tfDuration;
    private TextField tfArtist;
    private TextField tfYear;
    private TextField tfMedium;
    private TextField tfBrand;
    private TextField tfWarranty;
    private TextField tfMake;
    private TextField tfModel;
    private TextField tfVehicleYear;
    private Button btnDang;
    private VBox paneArtFields;
    private VBox paneElecFields;
    private VBox paneVehicleFields;
    private Label lblStatus;

    private Runnable onItemSaved;
    private Runnable onEditMode;

    public ItemFormManager(ItemDao itemDao, User currentSeller) {
        this.itemDao = itemDao;
        this.currentSeller = currentSeller;
    }

    public void setFormControls(ComboBox<String> cbCategory, TextField tfName, TextArea taDescription,
                                TextField tfPrice, TextField tfDuration, TextField tfArtist, TextField tfYear,
                                TextField tfMedium, TextField tfBrand, TextField tfWarranty, TextField tfMake,
                                TextField tfModel, TextField tfVehicleYear, Button btnDang, VBox paneArtFields,
                                VBox paneElecFields, VBox paneVehicleFields, Label lblStatus) {
        this.cbCategory = cbCategory;
        this.tfName = tfName;
        this.taDescription = taDescription;
        this.tfPrice = tfPrice;
        this.tfDuration = tfDuration;
        this.tfArtist = tfArtist;
        this.tfYear = tfYear;
        this.tfMedium = tfMedium;
        this.tfBrand = tfBrand;
        this.tfWarranty = tfWarranty;
        this.tfMake = tfMake;
        this.tfModel = tfModel;
        this.tfVehicleYear = tfVehicleYear;
        this.btnDang = btnDang;
        this.paneArtFields = paneArtFields;
        this.paneElecFields = paneElecFields;
        this.paneVehicleFields = paneVehicleFields;
        this.lblStatus = lblStatus;
    }

    public void setOnItemSaved(Runnable callback) {
        this.onItemSaved = callback;
    }

    public void setOnEditMode(Runnable callback) {
        this.onEditMode = callback;
    }

    public void onCategoryChanged() {
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

    public void onDangClick() {
        String loai = cbCategory.getValue();
        String ten = tfName.getText().trim();
        String moTa = taDescription.getText().trim();
        String giaText = tfPrice.getText().trim();

        if (loai == null) { showError("Lỗi: Chưa chọn loại sản phẩm!"); return; }
        if (ten.isEmpty()) { showError("Lỗi: Chưa nhập tên sản phẩm!"); return; }
        if (giaText.isEmpty()) { showError("Lỗi: Chưa nhập giá khởi điểm!"); return; }

        double gia;
        try {
            gia = Double.parseDouble(giaText);
            if (gia <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Lỗi: Giá khởi điểm không hợp lệ!");
            return;
        }

        if (dangSuaId == -1) {
            themSanPhamMoi(loai, ten, moTa, gia);
        } else {
            capNhatSanPham(ten, moTa, gia);
        }
    }

    public void chuyenSangSuaSanPham(ItemRow row) {
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

        btnDang.setText("Cập nhật sản phẩm");
        showInfo("Đang sửa sản phẩm: " + record.item.getName());

        if (onEditMode != null) {
            onEditMode.run();
        }
    }

    private void themSanPhamMoi(String loai, String ten, String moTa, double gia) {
        if (currentSeller == null) {
            showError("❌ Lỗi: Không tìm thấy thông tin người bán. Vui lòng đăng nhập lại!");
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
                showError("Loại sản phẩm không hợp lệ!");
                return;
            }
        }

        int dbId = itemDao.saveItem(sanPham, currentSeller.getId());
        if (dbId > 0) {
            showSuccess("✅ Đăng sản phẩm thành công!");
            clearForm();
            if (onItemSaved != null) {
                onItemSaved.run();
            }
        } else {
            showError("❌ Lỗi: Không lưu được sản phẩm vào Database.");
        }
    }

    private void capNhatSanPham(String ten, String moTa, double gia) {
        boolean ketQua = itemDao.updateItemInfo(dangSuaId, ten, moTa, gia);
        if (ketQua) {
            showSuccess("✅ Cập nhật sản phẩm thành công!");
            clearForm();
            dangSuaId = -1;
            btnDang.setText("Đăng sản phẩm");
            if (onItemSaved != null) {
                onItemSaved.run();
            }
        } else {
            showError("❌ Không thể cập nhật! Có thể sản phẩm đang RUNNING.");
        }
    }

    public void clearForm() {
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

    public void resetForm() {
        clearForm();
        dangSuaId = -1;
        btnDang.setText("Đăng sản phẩm");
        lblStatus.setText("");
    }

    public int getDangSuaId() {
        return dangSuaId;
    }

    public void setDangSuaId(int id) {
        this.dangSuaId = id;
    }

    private void showError(String msg) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
    }

    private void showSuccess(String msg) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
    }

    private void showInfo(String msg) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-text-fill: #6E1C1C; -fx-font-size: 12px;");
    }
}
