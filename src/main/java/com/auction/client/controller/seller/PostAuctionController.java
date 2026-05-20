package com.auction.client.controller.seller;

import com.auction.client.controller.auth.UserSession;
import com.auction.client.networkclient.ClientSocket;
import com.auction.client.util.CloudStorageUtil;
import com.auction.common.dto.ItemDTOs;
import com.auction.common.enums.ActionType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class PostAuctionController {

    private static final Logger logger = LoggerFactory.getLogger(PostAuctionController.class);

    // --- Các trường nhập liệu của Form bên trái ---
    @FXML private TextField txtProductName;
    @FXML private ComboBox<String> cbCategory;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtStartingPrice;
    @FXML private TextField txtIncrement;
    @FXML private TextField txtBuyNowPrice;
    @FXML private DatePicker dpStartDate;
    @FXML private TextField txtStartTime;
    @FXML private DatePicker dpEndDate;
    @FXML private TextField txtEndTime;
    @FXML private CheckBox chkAntiSniping;
    @FXML private CheckBox chkCommit;

    // --- Các thành phần hiển thị Realtime Xem Trước (Preview) ---
    @FXML private Label lblPreviewName;
    @FXML private Label lblPreviewCategory;
    @FXML private Label lblPreviewPrice;
    @FXML private Label lblPreviewIncrement;
    @FXML private Label lblPreviewBuyNow;
    @FXML private Label lblPreviewTime;
    @FXML private Label lblPreviewAntiSniping;
    @FXML private ImageView imgPreview;
    @FXML private Button btnUploadImage;

    // --- Các thành phần thông tin tài khoản góc phải trên (Profile) ---
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileRole;

    private String selectedImagePath = "";
    private File selectedImageFile = null;

    @FXML
    public void initialize() {
        if (cbCategory != null) {
            cbCategory.getItems().clear();
            cbCategory.getItems().addAll("Điện tử", "Xe cộ", "Nghệ thuật", "Khác");
        }
        setupLivePreview();
        logger.info("Seller Dashboard initialized - Đã đồng bộ toàn bộ form và xem trước.");
    }

    private void setupLivePreview() {
        if (lblProfileName != null && UserSession.getUsername() != null) {
            lblProfileName.setText("Chào, " + UserSession.getUsername());
        }
        if (lblProfileRole != null && UserSession.getCurrentRole() != null) {
            String role = UserSession.getCurrentRole();
            lblProfileRole.setText(role.substring(0, 1).toUpperCase() + role.substring(1).toUpperCase());
        }

        if (txtProductName != null && lblPreviewName != null) {
            txtProductName.textProperty().addListener((obs, oldVal, newVal) -> {
                lblPreviewName.setText(newVal.isEmpty() ? "Tên sản phẩm mẫu..." : newVal);
            });
        }

        if (cbCategory != null && lblPreviewCategory != null) {
            cbCategory.valueProperty().addListener((obs, oldVal, newVal) -> {
                lblPreviewCategory.setText(newVal != null ? newVal : "Chưa chọn");
            });
        }

        // Lắng nghe Giá khởi điểm
        if (txtStartingPrice != null && lblPreviewPrice != null) {
            txtStartingPrice.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.isEmpty()) {
                    lblPreviewPrice.setText("0 đ");
                } else {
                    try {
                        String cleanString = newVal.replaceAll("[^\\d]", "");
                        double price = Double.parseDouble(cleanString);
                        lblPreviewPrice.setText(String.format("%,.0f đ", price));
                    } catch (NumberFormatException e) {
                        lblPreviewPrice.setText("Giá không hợp lệ");
                    }
                }
            });
        }

        // Lắng nghe Bước giá
        if (txtIncrement != null && lblPreviewIncrement != null) {
            txtIncrement.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.isEmpty()) {
                    lblPreviewIncrement.setText("0 đ");
                } else {
                    try {
                        String cleanString = newVal.replaceAll("[^\\d]", "");
                        double price = Double.parseDouble(cleanString);
                        lblPreviewIncrement.setText(String.format("%,.0f đ", price));
                    } catch (NumberFormatException e) {
                        lblPreviewIncrement.setText("Giá không hợp lệ");
                    }
                }
            });
        }

        // Lắng nghe Giá mua đứt (Chống lỗi Thread-safety)
        if (txtBuyNowPrice != null && lblPreviewBuyNow != null) {
            txtBuyNowPrice.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.isEmpty()) {
                    lblPreviewBuyNow.setText("Không có");
                } else {
                    try {
                        String cleanString = newVal.replaceAll("[^\\d]", "");
                        double price = Double.parseDouble(cleanString);
                        lblPreviewBuyNow.setText(String.format("%,.0f đ", price));
                    } catch (NumberFormatException e) {
                        lblPreviewBuyNow.setText("Giá không hợp lệ");
                    }
                }
            });
        }

        javafx.beans.InvalidationListener timeListener = obs -> {
            String startDate = (dpStartDate != null && dpStartDate.getValue() != null)
                    ? dpStartDate.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "??/??/????";
            String startTime = (txtStartTime != null && !txtStartTime.getText().isEmpty())
                    ? txtStartTime.getText() : "--:--";

            String endDate = (dpEndDate != null && dpEndDate.getValue() != null)
                    ? dpEndDate.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "??/??/????";
            String endTime = (txtEndTime != null && !txtEndTime.getText().isEmpty())
                    ? txtEndTime.getText() : "--:--";

            if (lblPreviewTime != null) {
                lblPreviewTime.setText(String.format("%s %s\nđến %s %s", startDate, startTime, endDate, endTime));
            }
        };

        if (dpStartDate != null) dpStartDate.valueProperty().addListener(timeListener);
        if (txtStartTime != null) txtStartTime.textProperty().addListener(timeListener);
        if (dpEndDate != null) dpEndDate.valueProperty().addListener(timeListener);
        if (txtEndTime != null) txtEndTime.textProperty().addListener(timeListener);

        if (chkAntiSniping != null && lblPreviewAntiSniping != null) {
            lblPreviewAntiSniping.setText(chkAntiSniping.isSelected() ? "Có áp dụng" : "Không áp dụng");
            chkAntiSniping.selectedProperty().addListener((obs, oldVal, isChecked) -> {
                lblPreviewAntiSniping.setText(isChecked ? "Có áp dụng" : "Không áp dụng");
            });
        }
    }

    @FXML
    public void handleClearForm(ActionEvent event) {
        if (txtProductName != null) txtProductName.clear();
        if (txtStartingPrice != null) txtStartingPrice.clear();
        if (txtIncrement != null) txtIncrement.clear();
        if (txtBuyNowPrice != null) txtBuyNowPrice.clear();
        if (txtDescription != null) txtDescription.clear();
        if (cbCategory != null) cbCategory.getSelectionModel().clearSelection();
        if (dpStartDate != null) dpStartDate.setValue(null);
        if (txtStartTime != null) txtStartTime.clear();
        if (dpEndDate != null) dpEndDate.setValue(null);
        if (txtEndTime != null) txtEndTime.clear();
        if (chkAntiSniping != null) chkAntiSniping.setSelected(false);
        if (chkCommit != null) chkCommit.setSelected(false);

        selectedImagePath = "";
        selectedImageFile = null;
        if (imgPreview != null) imgPreview.setImage(null);
        logger.info("Đã làm sạch form nhập liệu.");
    }

    @FXML
    public void handleUploadImage(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm đấu giá");
        fileChooser.getExtensionFilters().addAll(new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        java.io.File selectedFile = fileChooser.showOpenDialog(((javafx.scene.Node) event.getSource()).getScene().getWindow());

        if (selectedFile != null) {
            this.selectedImageFile = selectedFile;
            this.selectedImagePath = selectedFile.toURI().toString();
            javafx.scene.image.Image image = new javafx.scene.image.Image(this.selectedImagePath);
            if (imgPreview != null) imgPreview.setImage(image);
        }
    }

    @FXML
    public void handleSubmitAuction(ActionEvent event) {
        if (isInputInvalid()) return;

        if (this.selectedImageFile == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu ảnh", "Vui lòng chọn ảnh cho sản phẩm!");
            return;
        }

        try {
            String name = txtProductName.getText();
            long startPrice = Long.parseLong(txtStartingPrice.getText().replaceAll("[^\\d]", ""));
            long increment = (txtIncrement != null && !txtIncrement.getText().isEmpty()) ? Long.parseLong(txtIncrement.getText().replaceAll("[^\\d]", "")) : 0;
            long buyNow = (txtBuyNowPrice != null && !txtBuyNowPrice.getText().isEmpty()) ? Long.parseLong(txtBuyNowPrice.getText().replaceAll("[^\\d]", "")) : 0;

            String category = mapCategoryToEnum(cbCategory.getValue());
            String description = txtDescription.getText() != null ? txtDescription.getText() : "";

            LocalDateTime startTime = parseDateTime(dpStartDate, txtStartTime);
            LocalDateTime endTime = parseDateTime(dpEndDate, txtEndTime);

            if (endTime.isBefore(startTime)) {
                showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian kết thúc phải lớn hơn thời gian bắt đầu!");
                return;
            }

            // Kiểm tra tính hợp lệ về logic tài chính
            if (buyNow > 0 && buyNow <= startPrice) {
                showAlert(Alert.AlertType.WARNING, "Lỗi cấu hình giá", "Giá mua đứt phải lớn hơn giá khởi điểm!");
                return;
            }

            String startTimeStr = startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String endTimeStr = endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            CloudStorageUtil.uploadImageAsync(this.selectedImageFile).thenAccept(imageUrl -> {
                Platform.runLater(() -> {
                    if (imageUrl == null) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể tải ảnh lên hệ thống đám mây!");
                        return;
                    }

                    // Khởi tạo DTO cơ bản
                    ItemDTOs.CreateItemRequest requestDto = new ItemDTOs.CreateItemRequest(
                            name, description, startPrice, category, 0, imageUrl, startTimeStr, endTimeStr);

                    // Bơm thêm các thuộc tính mở rộng bằng JSON thao tác trực tiếp (Giải pháp linh hoạt)
                    JsonObject reqJson = new Gson().toJsonTree(requestDto).getAsJsonObject();
                    reqJson.addProperty("bidIncrement", increment);
                    reqJson.addProperty("buyNowPrice", buyNow);
                    reqJson.addProperty("type", ActionType.CREATE_PRODUCT);

                    ClientSocket.getInstance().sendJsonRequest(reqJson, "CREATE_ITEM_RESPONSE", response -> {
                        Platform.runLater(() -> {
                            boolean success = response.has("success") && response.get("success").getAsBoolean();
                            if (success) {
                                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Sản phẩm đã lên sàn đấu giá!");
                                handleClearForm(null);
                            } else {
                                String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi hệ thống!";
                                showAlert(Alert.AlertType.ERROR, "Thất bại", msg);
                            }
                        });
                    });
                });
            });

        } catch (DateTimeParseException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Vui lòng nhập đúng định dạng giờ HH:mm (VD: 14:30)");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Kiểm tra lại dữ liệu số tiền!");
        }
    }

    private boolean isInputInvalid() {
        if (txtProductName.getText() == null || txtProductName.getText().trim().isEmpty() ||
                txtStartingPrice.getText() == null || txtStartingPrice.getText().trim().isEmpty() ||
                cbCategory.getValue() == null || dpStartDate.getValue() == null || dpEndDate.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng điền đầy đủ thông tin bắt buộc!");
            return true;
        }
        if (chkCommit != null && !chkCommit.isSelected()) {
            showAlert(Alert.AlertType.WARNING, "Chưa xác nhận", "Bạn phải tích chọn cam kết thông tin chuẩn xác.");
            return true;
        }
        return false;
    }

    private String mapCategoryToEnum(String uiCategory) {
        if (uiCategory == null) return "OTHER";
        switch (uiCategory) {
            case "Điện tử": return "ELECTRONICS";
            case "Xe cộ": return "VEHICLE";
            case "Nghệ thuật": return "ART";
            default: return "OTHER";
        }
    }

    private LocalDateTime parseDateTime(DatePicker datePicker, TextField timeField) throws DateTimeParseException {
        LocalDate date = datePicker.getValue();
        if (date == null) throw new DateTimeParseException("Chưa chọn ngày", "", 0);
        String timeStr = timeField.getText().trim();
        LocalTime time = LocalTime.of(0, 0);
        if (!timeStr.isEmpty()) {
            if (timeStr.length() == 5 && timeStr.contains(":")) {
                String[] parts = timeStr.split(":");
                time = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            } else {
                throw new DateTimeParseException("Sai định dạng giờ (HH:mm)", timeStr, 0);
            }
        }
        return LocalDateTime.of(date, time);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}