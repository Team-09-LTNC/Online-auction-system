package com.auction.server.manager;

import com.auction.common.model.user.User;
import com.auction.server.dao.ItemDao;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.List;

public class ItemListManager {
    private ItemDao itemDao;
    private User currentSeller;
    private ObservableList<ItemRow> danhSachSanPham;
    private Runnable onListUpdated;

    public ItemListManager(ItemDao itemDao, User currentSeller, ObservableList<ItemRow> danhSachSanPham) {
        this.itemDao = itemDao;
        this.currentSeller = currentSeller;
        this.danhSachSanPham = danhSachSanPham;
    }

    public void setOnListUpdated(Runnable callback) {
        this.onListUpdated = callback;
    }

    public void loadDanhSach() {
        if (itemDao == null) {
            showError("Lỗi: Không có kết nối Database!");
            return;
        }
        if (currentSeller == null) {
            showError("Lỗi: Chưa xác định người bán!");
            return;
        }

        danhSachSanPham.clear();
        List<ItemDao.ItemRecord> records = itemDao.findBySellerId(currentSeller.getId());

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

    public void xoaSanPham(ItemRow row) {
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

    private void showError(String msg) {
        if (onListUpdated != null) {
            onListUpdated.run();
        }
    }
}
