package com.auction.client.controller.seller;

import com.auction.client.controller.auth.UserSession;
import com.auction.client.networkclient.ClientSocket;
import com.auction.client.util.CloudStorageUtil;
import com.auction.common.dto.ItemDTOs;
import com.auction.common.enums.ActionType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostAuctionController {
  private static final Logger logger = LoggerFactory.getLogger(PostAuctionController.class);

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

  @FXML private Label lblPreviewName;
  @FXML private Label lblPreviewCategory;
  @FXML private Label lblPreviewPrice;
  @FXML private Label lblPreviewIncrement;
  @FXML private Label lblPreviewBuyNow;
  @FXML private Label lblPreviewTime;
  @FXML private Label lblPreviewAntiSniping;
  @FXML private ImageView imgPreview;
  @FXML private Button btnUploadImage;

  @FXML private Label lblProfileName;
  @FXML private Label lblProfileRole;

  private String selectedImagePath = "";
  private File selectedImageFile;
  private volatile boolean isSubmitting = false;

  @FXML
  public void initialize() {
    if (cbCategory != null) {
      cbCategory.getItems().clear();
      cbCategory.getItems().addAll("Điện tử", "Xe cộ", "Nghệ thuật", "Khác");
    }
    setupLivePreview();
    logger.info("Seller Dashboard initialized - da dong bo form va preview.");
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
      txtProductName.textProperty().addListener((obs, oldVal, newVal) ->
          lblPreviewName.setText(newVal.isEmpty() ? "Tên sản phẩm mẫu..." : newVal));
    }
    if (cbCategory != null && lblPreviewCategory != null) {
      cbCategory.valueProperty().addListener((obs, oldVal, newVal) ->
          lblPreviewCategory.setText(newVal != null ? newVal : "Chưa chọn"));
    }
    bindCurrencyPreview(txtStartingPrice, lblPreviewPrice);
    bindCurrencyPreview(txtIncrement, lblPreviewIncrement);
    bindBuyNowPreview();
    bindTimePreview();
    bindAntiSnipingPreview();
  }

  private void bindCurrencyPreview(TextField source, Label target) {
    if (source == null || target == null) {
      return;
    }
    source.textProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal == null || newVal.isEmpty()) {
        target.setText("0 đ");
        return;
      }
      try {
        String clean = newVal.replaceAll("[^\\d]", "");
        double price = Double.parseDouble(clean);
        target.setText(String.format("%,.0f đ", price));
      } catch (NumberFormatException e) {
        target.setText("Giá không hợp lệ");
      }
    });
  }

  private void bindBuyNowPreview() {
    if (txtBuyNowPrice == null || lblPreviewBuyNow == null) {
      return;
    }
    txtBuyNowPrice.textProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal == null || newVal.isEmpty()) {
        lblPreviewBuyNow.setText("Không có");
        return;
      }
      try {
        String clean = newVal.replaceAll("[^\\d]", "");
        double price = Double.parseDouble(clean);
        lblPreviewBuyNow.setText(String.format("%,.0f đ", price));
      } catch (NumberFormatException e) {
        lblPreviewBuyNow.setText("Giá không hợp lệ");
      }
    });
  }

  private void bindTimePreview() {
    javafx.beans.InvalidationListener listener = obs -> {
      String startDate = (dpStartDate != null && dpStartDate.getValue() != null)
          ? dpStartDate.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
          : "??/??/????";
      String startTime = (txtStartTime != null && !txtStartTime.getText().isEmpty())
          ? txtStartTime.getText() : "--:--";
      String endDate = (dpEndDate != null && dpEndDate.getValue() != null)
          ? dpEndDate.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
          : "??/??/????";
      String endTime = (txtEndTime != null && !txtEndTime.getText().isEmpty())
          ? txtEndTime.getText() : "--:--";
      if (lblPreviewTime != null) {
        lblPreviewTime.setText(String.format("%s %s\nđến %s %s", startDate, startTime, endDate, endTime));
      }
    };
    if (dpStartDate != null) {
      dpStartDate.valueProperty().addListener(listener);
    }
    if (txtStartTime != null) {
      txtStartTime.textProperty().addListener(listener);
    }
    if (dpEndDate != null) {
      dpEndDate.valueProperty().addListener(listener);
    }
    if (txtEndTime != null) {
      txtEndTime.textProperty().addListener(listener);
    }
  }

  private void bindAntiSnipingPreview() {
    if (chkAntiSniping == null || lblPreviewAntiSniping == null) {
      return;
    }
    lblPreviewAntiSniping.setText(chkAntiSniping.isSelected() ? "Có áp dụng" : "Không áp dụng");
    chkAntiSniping.selectedProperty().addListener((obs, oldVal, checked) ->
        lblPreviewAntiSniping.setText(checked ? "Có áp dụng" : "Không áp dụng"));
  }

  @FXML
  public void handleClearForm(ActionEvent event) {
    clearField(txtProductName);
    clearField(txtStartingPrice);
    clearField(txtIncrement);
    clearField(txtBuyNowPrice);
    if (txtDescription != null) {
      txtDescription.clear();
    }
    if (cbCategory != null) {
      cbCategory.getSelectionModel().clearSelection();
    }
    if (dpStartDate != null) {
      dpStartDate.setValue(null);
    }
    clearField(txtStartTime);
    if (dpEndDate != null) {
      dpEndDate.setValue(null);
    }
    clearField(txtEndTime);
    if (chkAntiSniping != null) {
      chkAntiSniping.setSelected(false);
    }
    if (chkCommit != null) {
      chkCommit.setSelected(false);
    }
    selectedImagePath = "";
    selectedImageFile = null;
    if (imgPreview != null) {
      imgPreview.setImage(null);
    }
    logger.info("Da lam sach form nhap lieu.");
  }

  private void clearField(TextField textField) {
    if (textField != null) {
      textField.clear();
    }
  }

  @FXML
  public void handleUploadImage(ActionEvent event) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Chọn ảnh sản phẩm đấu giá");
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
    File selectedFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
    if (selectedFile == null) {
      return;
    }
    selectedImageFile = selectedFile;
    selectedImagePath = selectedFile.toURI().toString();
    Image image = new Image(selectedImagePath);
    if (imgPreview != null) {
      imgPreview.setImage(image);
    }
  }

  @FXML
  public void handleSubmitAuction(ActionEvent event) {
    Button submitButton = event != null && event.getSource() instanceof Button
        ? (Button) event.getSource()
        : null;
    if (isSubmitting) {
      return;
    }
    if (isInputInvalid()) {
      return;
    }
    if (selectedImageFile == null) {
      showAlert(Alert.AlertType.WARNING, "Thiếu ảnh", "Vui lòng chọn ảnh cho sản phẩm!");
      return;
    }

    try {
      String name = txtProductName.getText();
      long startPrice = parseMoney(txtStartingPrice.getText());
      if (startPrice <= 0) {
        showAlert(Alert.AlertType.WARNING, "Giá trị không hợp lệ", "Giá khởi điểm phải lớn hơn 0 đ!");
        return;
      }
      long increment = parseMoney(txtIncrement.getText());
      long buyNow = parseOptionalMoney(txtBuyNowPrice);
      if (increment <= 0) {
        showAlert(Alert.AlertType.WARNING, "Giá trị không hợp lệ", "Bước giá phải lớn hơn 0 đ!");
        return;
      }
      if (buyNow > 0 && buyNow <= startPrice) {
        showAlert(Alert.AlertType.WARNING, "Lỗi cấu hình giá", "Giá mua đứt phải lớn hơn giá khởi điểm!");
        return;
      }

      String category = mapCategoryToEnum(cbCategory.getValue());
      String description = txtDescription.getText() != null ? txtDescription.getText() : "";
      LocalDateTime startTime = parseDateTime(dpStartDate, txtStartTime);
      LocalDateTime endTime = parseDateTime(dpEndDate, txtEndTime);

      if (!startTime.isAfter(LocalDateTime.now())) {
        showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian bắt đầu phải lớn hơn hiện tại!");
        return;
      }
      if (!endTime.isAfter(startTime)) {
        showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian kết thúc phải lớn hơn bắt đầu!");
        return;
      }

      isSubmitting = true;
      setSubmitButtonState(submitButton, true);
      String startTimeStr = startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      String endTimeStr = endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

      CloudStorageUtil.uploadImageAsync(selectedImageFile).thenAccept(imageUrl ->
          Platform.runLater(() -> onUploadSuccess(
              imageUrl, name, description, startPrice, increment, buyNow, category,
              startTime, startTimeStr, endTimeStr, submitButton)))
          .exceptionally(ex -> {
            Platform.runLater(() -> {
              isSubmitting = false;
              setSubmitButtonState(submitButton, false);
              showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể tải ảnh lên đám mây!");
            });
            return null;
          });
    } catch (DateTimeParseException e) {
      isSubmitting = false;
      setSubmitButtonState(submitButton, false);
      showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Vui lòng nhập giờ đúng dạng HH:mm (VD: 14:30)");
    } catch (Exception e) {
      isSubmitting = false;
      setSubmitButtonState(submitButton, false);
      showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Kiểm tra lại dữ liệu số tiền!");
    }
  }

  private void onUploadSuccess(
      String imageUrl,
      String name,
      String description,
      long startPrice,
      long increment,
      long buyNow,
      String category,
      LocalDateTime startTime,
      String startTimeStr,
      String endTimeStr,
      Button submitButton
  ) {
    if (imageUrl == null) {
      showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể tải ảnh lên đám mây!");
      isSubmitting = false;
      setSubmitButtonState(submitButton, false);
      return;
    }

    ItemDTOs.CreateItemRequest requestDto =
        new ItemDTOs.CreateItemRequest(name, description, startPrice, category, 0, imageUrl, startTimeStr, endTimeStr);
    JsonObject reqJson = new Gson().toJsonTree(requestDto).getAsJsonObject();
    reqJson.addProperty("bidIncrement", increment);
    reqJson.addProperty("buyNowPrice", buyNow);
    reqJson.addProperty("antiSnipingEnabled", chkAntiSniping != null && chkAntiSniping.isSelected());
    reqJson.addProperty("type", ActionType.CREATE_PRODUCT);
    reqJson.addProperty("requestId", UUID.randomUUID().toString());
    reqJson.addProperty("status", !startTime.isAfter(LocalDateTime.now()) ? "RUNNING" : "OPEN");

    ClientSocket.getInstance().sendJsonRequest(reqJson, ActionType.CREATE_PRODUCT, response ->
        Platform.runLater(() -> {
          isSubmitting = false;
          setSubmitButtonState(submitButton, false);
          boolean success = response.has("success") && response.get("success").getAsBoolean();
          if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Sản phẩm đã lên sàn đấu giá!");
            handleClearForm(null);
          } else {
            String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi hệ thống!";
            showAlert(Alert.AlertType.ERROR, "Thất bại", msg);
          }
        }));
  }

  private long parseMoney(String value) {
    return Long.parseLong(value.replaceAll("[^\\d]", ""));
  }

  private long parseOptionalMoney(TextField field) {
    if (field == null || field.getText() == null || field.getText().isEmpty()) {
      return 0L;
    }
    return parseMoney(field.getText());
  }

  private void setSubmitButtonState(Button button, boolean submitting) {
    if (button == null) {
      return;
    }
    button.setDisable(submitting);
    button.setText(submitting ? "ĐANG ĐĂNG..." : "ĐĂNG SẢN PHẨM");
  }

  private boolean isInputInvalid() {
    if (txtProductName.getText() == null || txtProductName.getText().trim().isEmpty()
        || txtStartingPrice.getText() == null || txtStartingPrice.getText().trim().isEmpty()
        || txtIncrement == null || txtIncrement.getText() == null || txtIncrement.getText().trim().isEmpty()
        || txtStartTime == null || txtStartTime.getText() == null || txtStartTime.getText().trim().isEmpty()
        || txtEndTime == null || txtEndTime.getText() == null || txtEndTime.getText().trim().isEmpty()
        || cbCategory.getValue() == null || dpStartDate.getValue() == null || dpEndDate.getValue() == null) {
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
    if (uiCategory == null) {
      return "OTHER";
    }
    switch (uiCategory) {
      case "Điện tử":
        return "ELECTRONICS";
      case "Xe cộ":
        return "VEHICLE";
      case "Nghệ thuật":
        return "ART";
      default:
        return "OTHER";
    }
  }

  private LocalDateTime parseDateTime(DatePicker datePicker, TextField timeField) throws DateTimeParseException {
    LocalDate date = datePicker.getValue();
    if (date == null) {
      throw new DateTimeParseException("Chua chon ngay", "", 0);
    }
    String timeStr = timeField.getText().trim();
    LocalTime time = LocalTime.of(0, 0);
    if (!timeStr.isEmpty()) {
      if (timeStr.length() == 5 && timeStr.contains(":")) {
        String[] parts = timeStr.split(":");
        time = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
      } else {
        throw new DateTimeParseException("Sai dinh dang gio (HH:mm)", timeStr, 0);
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
