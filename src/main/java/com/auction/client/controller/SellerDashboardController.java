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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller màn hình quản lý sản phẩm của Seller.
 *
 * Phân chia trách nhiệm (Single Responsibility):
 *   - Controller      : điều phối UI, chuyển scene, xử lý sự kiện FXML
 *   - ItemListManager : tải danh sách, xóa sản phẩm
 *   - ItemFormManager : điền form, validate, lưu / cập nhật sản phẩm
 */
public class SellerDashboardController {

    // =========================================================================
    // FXML — HEADER
    // =========================================================================
    @FXML private Label  lblWelcome;
    @FXML private Label  lblStatus;
    @FXML private Button btnLogout;

    // =========================================================================
    // FXML — MENU SIDEBAR
    // =========================================================================
    @FXML private Button btnMenuMyItems;
    @FXML private Button btnMenuAddNew;

    // =========================================================================
    // FXML — PANE DANH SÁCH
    // =========================================================================
    @FXML private VBox paneMyItems;

    @FXML private TableView<ItemRow>           itemTable;
    @FXML private TableColumn<ItemRow, String> colName;
    @FXML private TableColumn<ItemRow, String> colCategory;
    @FXML private TableColumn<ItemRow, String> colPrice;
    @FXML private TableColumn<ItemRow, String> colStatus;
    @FXML private TableColumn<ItemRow, Void>   colAction;

    // =========================================================================
    // FXML — PANE FORM ĐĂNG / SỬA SẢN PHẨM
    // =========================================================================
    @FXML private ScrollPane paneAddNew;

    // Form chung
    @FXML private ComboBox<String> cbCategory;
    @FXML private TextField        tfName;
    @FXML private TextArea         taDescription;
    @FXML private TextField        tfPrice;
    @FXML private TextField        tfDuration;
    @FXML private Button           btnDang;

    // Sub-pane ART
    @FXML private VBox      paneArtFields;
    @FXML private TextField tfArtist;
    @FXML private TextField tfYear;
    @FXML private TextField tfMedium;

    // Sub-pane ELECTRONICS
    @FXML private VBox      paneElecFields;
    @FXML private TextField tfBrand;
    @FXML private TextField tfWarranty;

    // Sub-pane VEHICLE
    @FXML private VBox      paneVehicleFields;
    @FXML private TextField tfMake;
    @FXML private TextField tfModel;
    @FXML private TextField tfVehicleYear;

    // =========================================================================
    // FIELDS NỘI BỘ
    // =========================================================================
    private User                    currentSeller;
    private ItemDao                 itemDao;
    private ObservableList<ItemRow> danhSachSanPham;
    private ItemListManager         itemListManager;
    private ItemFormManager         itemFormManager;

    // =========================================================================
    // INITIALIZE — JavaFX gọi tự động sau khi nạp FXML
    // =========================================================================
    @FXML
    public void initialize() {
        // Kết nối Database
        java.sql.Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            this.itemDao = new ItemDao(conn);
            System.out.println(">>> [OK] SellerDashboard kết nối Database thành công.");
        } else {
            hienThiLoi("❌ Không thể kết nối Database. Kiểm tra Internet hoặc IP Whitelist!");
        }

        // Khởi tạo ObservableList và cấu hình TableView
        danhSachSanPham = FXCollections.observableArrayList();
        cbCategory.getItems().addAll("ART", "ELECTRONICS", "VEHICLE");

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        setupCotHanhDong();
        itemTable.setItems(danhSachSanPham);

        // Mặc định hiện pane danh sách
        hienPaneMyItems();
    }

    // =========================================================================
    // KHỞI TẠO DỮ LIỆU SELLER — gọi từ LoginController sau khi load FXML
    // =========================================================================
    public void initSeller(User seller) {
        this.currentSeller = seller;
        lblWelcome.setText("Xin chào, " + seller.getFullName() + "!");

        // Khởi tạo 2 manager, truyền đúng dependencies
        this.itemListManager = new ItemListManager(itemDao, currentSeller, danhSachSanPham);
        this.itemFormManager = new ItemFormManager(itemDao, currentSeller);

        // Truyền toàn bộ control form vào ItemFormManager
        itemFormManager.setFormControls(
                cbCategory, tfName, taDescription, tfPrice, tfDuration,
                tfArtist, tfYear, tfMedium,
                tfBrand, tfWarranty,
                tfMake, tfModel, tfVehicleYear,
                btnDang,
                paneArtFields, paneElecFields, paneVehicleFields,
                lblStatus
        );

        // Callback: sau khi lưu/cập nhật xong → tải lại danh sách + quay về pane danh sách
        itemFormManager.setOnItemSaved(() -> {
            itemListManager.loadDanhSach();
            hienPaneMyItems();
        });

        // Callback: khi chuyển sang chế độ sửa → mở pane form
        itemFormManager.setOnEditMode(this::hienPaneAddNew);

        // Tải danh sách sản phẩm lần đầu
        if (itemDao != null) {
            itemListManager.loadDanhSach();
        }
    }

    // =========================================================================
    // MENU SIDEBAR
    // =========================================================================

    @FXML
    private void onMenuMyItemsClick() {
        hienPaneMyItems();
        if (itemListManager != null) {
            itemListManager.loadDanhSach();
        }
    }

    @FXML
    private void onMenuAddNewClick() {
        if (itemFormManager != null) {
            itemFormManager.resetForm(); // xóa form và reset dangSuaId = -1
        }
        hienPaneAddNew();
    }
    // =========================================================================
// QUAY VỀ HOME
// =========================================================================
    @FXML
    private void onBackToHomeClick() {
        try {
            if (itemDao != null) {
                itemDao.closeConnection();
                itemDao = null;
            }

            Stage stage = (Stage) btnLogout.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"));
            Parent root = loader.load();

            // Cập nhật thanh auth của Home (vẫn đang đăng nhập)
            HomeController homeCtrl = loader.getController();
            homeCtrl.refreshAuthBar();

            stage.getScene().setRoot(root);
            stage.setTitle("HỆ THỐNG ĐẤU GIÁ");

        } catch (IOException e) {
            e.printStackTrace();
            hienThiLoi("❌ Lỗi: Không thể quay về trang chủ!");
        }
    }

    // =========================================================================
    // CỘT HÀNH ĐỘNG — Sửa / Xóa, chỉ hiện khi status = OPEN
    // =========================================================================
    private void setupCotHanhDong() {
        colAction.setCellFactory(col -> new TableCell<>() {
            final Button btnSua = new Button("Sửa");
            final Button btnXoa = new Button("Xóa");
            final HBox   box    = new HBox(5, btnSua, btnXoa);

            {
                btnSua.setStyle(
                        "-fx-background-color: #6E1C1C; -fx-text-fill: white; "
                        + "-fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
                btnXoa.setStyle(
                        "-fx-background-color: #c0392b; -fx-text-fill: white; "
                        + "-fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");

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
                    // Chỉ hiện nút hành động khi sản phẩm đang ở trạng thái OPEN
                    setGraphic("OPEN".equals(row.getStatus()) ? box : null);
                }
            }
        });
    }

    // =========================================================================
    // SỰ KIỆN FORM — Delegate xuống ItemFormManager
    // =========================================================================

    /** Khi user thay đổi loại sản phẩm ở ComboBox */
    @FXML
    private void onCategoryChanged() {
        if (itemFormManager != null) {
            itemFormManager.onCategoryChanged();
        }
    }

    /** Khi user click "Đăng sản phẩm" hoặc "Cập nhật sản phẩm" */
    @FXML
    private void onDangClick() {
        if (itemFormManager != null) {
            itemFormManager.onDangClick();
        }
    }

    /** Khi user click "Xóa form" */
    @FXML
    private void onClearFormClick() {
        if (itemFormManager != null) {
            itemFormManager.resetForm();
        }
    }

    /** Khi user click "🔄 Tải lại" */
    @FXML
    private void onRefreshClick() {
        if (itemListManager != null) {
            itemListManager.loadDanhSach();
            hienThiThongBao("🔄 Đã tải lại danh sách.");
        }
    }

    // =========================================================================
    // ĐĂNG XUẤT — Xóa session, đóng DB, quay về Home.fxml
    // =========================================================================
    @FXML
    private void onLogoutClick() {
        // Bước 1: Đóng kết nối DB để tránh connection leak
        if (itemDao != null) {
            itemDao.closeConnection();
            itemDao = null;
        }

        // Bước 2: Xóa session đăng nhập toàn cục
        HomeController.Session.logout();

        // Bước 3: Chuyển về Home.fxml (KHÔNG phải Login.fxml)
        try {
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"));
            Parent root = loader.load();

            // Cập nhật thanh auth của Home về trạng thái "chưa đăng nhập"
            HomeController homeCtrl = loader.getController();
            homeCtrl.refreshAuthBar();

            stage.getScene().setRoot(root);
            stage.setTitle("HỆ THỐNG ĐẤU GIÁ");

        } catch (IOException e) {
            e.printStackTrace();
            hienThiLoi("❌ Lỗi: Không thể quay về màn hình chính!");
        }
    }

    // =========================================================================
    // HELPER — Chuyển đổi giữa 2 pane chính
    // =========================================================================
    private void hienPaneMyItems() {
        paneMyItems.setVisible(true);
        paneMyItems.setManaged(true);
        paneAddNew.setVisible(false);
        paneAddNew.setManaged(false);
        btnMenuMyItems.setStyle(styleMenuActive());
        btnMenuAddNew.setStyle(styleMenuInactive());
    }

    private void hienPaneAddNew() {
        paneAddNew.setVisible(true);
        paneAddNew.setManaged(true);
        paneMyItems.setVisible(false);
        paneMyItems.setManaged(false);
        btnMenuAddNew.setStyle(styleMenuActive());
        btnMenuMyItems.setStyle(styleMenuInactive());
    }

    // =========================================================================
    // HELPER — Style cho menu button
    // =========================================================================
    private String styleMenuActive() {
        return "-fx-background-color: #6E1C1C; -fx-text-fill: white; "
                + "-fx-font-size: 13px; -fx-padding: 12 15 12 15; "
                + "-fx-background-radius: 0; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";
    }

    private String styleMenuInactive() {
        return "-fx-background-color: transparent; -fx-text-fill: #1A0F0A; "
                + "-fx-font-size: 13px; -fx-padding: 12 15 12 15; "
                + "-fx-background-radius: 0; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";
    }

    // =========================================================================
    // HELPER — Hiển thị thông báo trên lblStatus
    // =========================================================================
    private void hienThiLoi(String msg) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
    }

    private void hienThiThongBao(String msg) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-text-fill: #6E1C1C; -fx-font-size: 12px;");
    }
}
