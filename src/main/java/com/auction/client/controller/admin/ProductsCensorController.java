package com.auction.client.controller.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * ProductsCensorController
 * ─────────────────────────────────────────────────────────────
 * Controller cho ProductsCensorView.fxml.
 * Hiển thị sản phẩm chưa duyệt; admin có thể duyệt hoặc xoá.
 */
public class ProductsCensorController implements Initializable {

    // ── FXML injections ──────────────────────────────────────
    @FXML private StackPane                        contentPane;
    @FXML private TableView<Product>               productTable;
    @FXML private TableColumn<Product, String>     colProductId;
    @FXML private TableColumn<Product, String>     colSellerId;
    @FXML private TableColumn<Product, String>     colName;
    @FXML private TableColumn<Product, String>     colDescription;
    @FXML private TableColumn<Product, String>     colCategory;
    @FXML private TableColumn<Product, String>     colStartPrice;
    @FXML private TableColumn<Product, String>     colImage;
    @FXML private Label                            lblProductCount;
    @FXML private TextField                        tfSearch;
    @FXML private ComboBox<String>                 cbCategory;
    @FXML private Button                           btnApprove;
    @FXML private Button                           btnDelete;

    // ── Data ─────────────────────────────────────────────────
    private final ObservableList<Product> masterList   = FXCollections.observableArrayList();
    private       FilteredList<Product>  filteredList;

    private static final List<String> CATEGORY_OPTIONS =
            List.of("Tất cả", "Điện tử", "Thời trang", "Đồ cổ", "Trang sức", "Xe cộ", "Khác");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        setupCategoryFilter();
        setupTable();
        loadData();
    }

    // ── Setup ────────────────────────────────────────────────

    private void setupColumns() {
        colProductId  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductId()));
        colSellerId   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSellerId()));
        colName       .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colCategory   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        colStartPrice .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStartPrice()));

        // Cột hình ảnh: hiển thị thumbnail nếu có URL, fallback về text
        colImage.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(60);
                imageView.setFitHeight(44);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    setText("—");
                } else {
                    try {
                        imageView.setImage(new Image(item, true));
                        setGraphic(imageView);
                        setText(null);
                    } catch (Exception e) {
                        setGraphic(null);
                        setText("(lỗi ảnh)");
                    }
                }
            }
        });
        colImage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getImageUrl()));
    }

    private void setupCategoryFilter() {
        cbCategory.setItems(FXCollections.observableArrayList(CATEGORY_OPTIONS));
        cbCategory.getSelectionModel().selectFirst();
    }

    private void setupTable() {
        filteredList = new FilteredList<>(masterList, p -> true);
        productTable.setItems(filteredList);
        productTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void loadData() {
        // TODO: thay bằng service call — chỉ lấy sản phẩm có status = PENDING
        masterList.setAll(
                new Product("SP-001", "SELLER-01", "Đồng hồ Rolex vintage",
                        "Đồng hồ cơ học năm 1972, còn mới 95%",
                        "Đồ cổ", "350.000.000", ""),
                new Product("SP-002", "SELLER-02", "iPhone 15 Pro Max 256GB",
                        "Máy mới 100%, còn bảo hành 11 tháng",
                        "Điện tử", "28.500.000", ""),
                new Product("SP-003", "SELLER-01", "Áo dài thêu tay",
                        "Chất liệu lụa Hà Đông, thêu tay truyền thống",
                        "Thời trang", "4.200.000", "")
        );
        updateCountLabel();
    }

    // ── FXML handlers ────────────────────────────────────────

    @FXML
    private void handleTableClick(MouseEvent e) {
        boolean selected = productTable.getSelectionModel().getSelectedItem() != null;
        btnApprove.setDisable(!selected);
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

    /** Duyệt sản phẩm được chọn. */
    @FXML
    private void handleApprove() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận duyệt");
        confirm.setHeaderText("Duyệt sản phẩm?");
        confirm.setContentText("Duyệt \"" + selected.getName() + "\" của seller " + selected.getSellerId() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            masterList.remove(selected); // TODO: gọi service cập nhật status → APPROVED
            resetButtons();
            updateCountLabel();
            showInfo("Đã duyệt", "Sản phẩm \"" + selected.getName() + "\" đã được duyệt thành công.");
        }
    }

    /** Xoá sản phẩm được chọn. */
    @FXML
    private void handleDelete() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xoá");
        confirm.setHeaderText("Xoá sản phẩm?");
        confirm.setContentText("Xoá \"" + selected.getName() + "\"? Hành động này không thể hoàn tác.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            masterList.remove(selected); // TODO: gọi service xoá
            resetButtons();
            updateCountLabel();
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private void applyFilter() {
        String kw  = tfSearch.getText().trim().toLowerCase();
        String cat = cbCategory.getValue();

        filteredList.setPredicate(p -> {
            boolean matchKw = kw.isEmpty()
                    || p.getProductId().toLowerCase().contains(kw)
                    || p.getName().toLowerCase().contains(kw)
                    || p.getSellerId().toLowerCase().contains(kw)
                    || p.getCategory().toLowerCase().contains(kw);
            boolean matchCat = (cat == null || cat.equals("Tất cả"))
                    || p.getCategory().equalsIgnoreCase(cat);
            return matchKw && matchCat;
        });
        updateCountLabel();
    }

    private void resetButtons() {
        btnApprove.setDisable(true);
        btnDelete.setDisable(true);
        productTable.getSelectionModel().clearSelection();
    }

    private void updateCountLabel() {
        int shown = filteredList.size();
        int total = masterList.size();
        lblProductCount.setText(shown == total
                ? total + " sản phẩm"
                : shown + " / " + total + " sản phẩm");
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── Model ────────────────────────────────────────────────

    public static class Product {
        private final SimpleStringProperty productId;
        private final SimpleStringProperty sellerId;
        private final SimpleStringProperty name;
        private final SimpleStringProperty description;
        private final SimpleStringProperty category;
        private final SimpleStringProperty startPrice;
        private final SimpleStringProperty imageUrl;

        public Product(String productId, String sellerId, String name,
                       String description, String category,
                       String startPrice, String imageUrl) {
            this.productId   = new SimpleStringProperty(productId);
            this.sellerId    = new SimpleStringProperty(sellerId);
            this.name        = new SimpleStringProperty(name);
            this.description = new SimpleStringProperty(description);
            this.category    = new SimpleStringProperty(category);
            this.startPrice  = new SimpleStringProperty(startPrice);
            this.imageUrl    = new SimpleStringProperty(imageUrl);
        }

        public String getProductId()   { return productId.get();   }
        public String getSellerId()    { return sellerId.get();     }
        public String getName()        { return name.get();         }
        public String getDescription() { return description.get();  }
        public String getCategory()    { return category.get();     }
        public String getStartPrice()  { return startPrice.get();   }
        public String getImageUrl()    { return imageUrl.get();     }

        public SimpleStringProperty productIdProperty()   { return productId;   }
        public SimpleStringProperty sellerIdProperty()    { return sellerId;    }
        public SimpleStringProperty nameProperty()        { return name;        }
        public SimpleStringProperty descriptionProperty() { return description; }
        public SimpleStringProperty categoryProperty()    { return category;    }
        public SimpleStringProperty startPriceProperty()  { return startPrice;  }
        public SimpleStringProperty imageUrlProperty()    { return imageUrl;    }
    }
}
