package com.auction.client.controller.seller;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.function.Consumer;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

final class MyProductEditDialog {
  void show(JsonObject itemObj, Consumer<Submission> submitHandler) {
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Sửa sản phẩm");
    dialog.setHeaderText("Cập nhật thông tin phiên đang chờ mở");
    ButtonType saveButton = new ButtonType("Lưu thay đổi", ButtonBar.ButtonData.OK_DONE);
    ButtonType cancelButton = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
    dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);
    dialog.getDialogPane().setPrefWidth(720);
    dialog.getDialogPane().setStyle("-fx-background-color: #FFFFFF;");
    Button saveButtonNode = (Button) dialog.getDialogPane().lookupButton(saveButton);
    Button cancelButtonNode = (Button) dialog.getDialogPane().lookupButton(cancelButton);
    saveButtonNode.setStyle(
        "-fx-background-color: #6A4C2B; -fx-text-fill: white; "
            + "-fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
    cancelButtonNode.setStyle(
        "-fx-background-color: #F4F0EC; -fx-text-fill: #4A3A2E; "
            + "-fx-background-radius: 8; -fx-padding: 8 14; -fx-cursor: hand;");

    TextField txtName = new TextField(MyProductsHelper.getString(itemObj, "name", ""));
    txtName.setPromptText("Tên sản phẩm");
    TextArea txtDescription = new TextArea(MyProductsHelper.getString(itemObj, "description", ""));
    txtDescription.setPrefRowCount(3);
    txtDescription.setPrefHeight(86);
    txtDescription.setWrapText(true);
    txtDescription.setPromptText("Mô tả sản phẩm");
    TextField txtPrice = new TextField(String.valueOf(MyProductsHelper.getLong(itemObj, "startingPrice", 0L)));
    txtPrice.setPromptText("Giá khởi điểm");
    ComboBox<String> categoryBox = new ComboBox<>();
    categoryBox.getItems().setAll("ELECTRONICS", "VEHICLE", "ART", "OTHER");
    categoryBox.setValue(MyProductsHelper.getString(itemObj, "category", "OTHER"));
    categoryBox.setMaxWidth(Double.MAX_VALUE);
    TextField txtImageUrl = new TextField(MyProductsHelper.getString(itemObj, "imageUrl", ""));
    txtImageUrl.setEditable(false);
    String currentImageThumbUrl = MyProductsHelper.getString(itemObj, "imageThumbUrl", txtImageUrl.getText());
    ImageView imagePreview = createImagePreview(currentImageThumbUrl);
    File[] selectedImageFile = new File[1];
    HBox imagePicker = createImagePicker(txtImageUrl, imagePreview, selectedImageFile);
    DatePicker dpStartDate = new DatePicker();
    TextField txtStartTime = new TextField();
    DatePicker dpEndDate = new DatePicker();
    TextField txtEndTime = new TextField();
    MyProductsHelper.fillDateTimeFields(
        MyProductsHelper.getString(itemObj, "startTime", null), dpStartDate, txtStartTime);
    MyProductsHelper.fillDateTimeFields(
        MyProductsHelper.getString(itemObj, "endTime", null), dpEndDate, txtEndTime);
    txtStartTime.setPromptText("HH:mm");
    txtEndTime.setPromptText("HH:mm");

    HBox form = createForm(
        txtName, txtDescription, txtPrice, categoryBox, imagePicker, imagePreview,
        dpStartDate, txtStartTime, dpEndDate, txtEndTime);
    dialog.getDialogPane().setContent(form);

    saveButtonNode.addEventFilter(ActionEvent.ACTION, event -> {
      event.consume();
      MyProductEditRequest request = new MyProductEditRequest(
          txtName.getText(),
          txtDescription.getText(),
          txtPrice.getText(),
          categoryBox.getValue(),
          txtImageUrl.getText(),
          currentImageThumbUrl,
          selectedImageFile[0],
          dpStartDate.getValue(),
          txtStartTime.getText(),
          dpEndDate.getValue(),
          txtEndTime.getText());
      Submission submission = new Submission(dialog, saveButtonNode, cancelButtonNode, form, request);
      submission.markUpdating();
      submitHandler.accept(submission);
    });

    dialog.showAndWait();
  }

  static final class Submission {
    private final Dialog<ButtonType> dialog;
    private final Button saveButton;
    private final Button cancelButton;
    private final Node form;
    private final MyProductEditRequest request;

    private Submission(
        Dialog<ButtonType> dialog,
        Button saveButton,
        Button cancelButton,
        Node form,
        MyProductEditRequest request
    ) {
      this.dialog = dialog;
      this.saveButton = saveButton;
      this.cancelButton = cancelButton;
      this.form = form;
      this.request = request;
    }

    MyProductEditRequest request() {
      return request;
    }

    void markUpdating() {
      saveButton.setText("Đang cập nhật sản phẩm...");
      saveButton.setDisable(true);
      cancelButton.setDisable(true);
      form.setDisable(true);
    }

    void reset() {
      saveButton.setText("Lưu thay đổi");
      saveButton.setDisable(false);
      cancelButton.setDisable(false);
      form.setDisable(false);
    }

    void close() {
      dialog.setResult(ButtonType.OK);
      dialog.close();
    }
  }

  private HBox createForm(
      TextField txtName,
      TextArea txtDescription,
      TextField txtPrice,
      ComboBox<String> categoryBox,
      HBox imagePicker,
      ImageView imagePreview,
      DatePicker dpStartDate,
      TextField txtStartTime,
      DatePicker dpEndDate,
      TextField txtEndTime
  ) {
    VBox leftColumn = new VBox(12);
    leftColumn.setPrefWidth(250);
    leftColumn.setMinWidth(250);
    leftColumn.getChildren().addAll(
        createImageSection(imagePreview, imagePicker),
        createSectionTitle("Giá và loại"),
        fieldBlock("Giá khởi điểm", txtPrice),
        fieldBlock("Loại", categoryBox)
    );

    VBox rightColumn = new VBox(12);
    HBox.setHgrow(rightColumn, Priority.ALWAYS);
    rightColumn.getChildren().addAll(
        createSectionTitle("Thông tin sản phẩm"),
        fieldBlock("Tên sản phẩm", txtName),
        fieldBlock("Mô tả", txtDescription),
        createSectionTitle("Thời gian phiên"),
        createTwoColumnRow(fieldBlock("Ngày bắt đầu", dpStartDate), fieldBlock("Giờ bắt đầu", txtStartTime)),
        createTwoColumnRow(fieldBlock("Ngày kết thúc", dpEndDate), fieldBlock("Giờ kết thúc", txtEndTime))
    );

    HBox content = new HBox(18, leftColumn, rightColumn);
    content.setPadding(new Insets(14));
    content.setAlignment(Pos.TOP_LEFT);
    content.setStyle("-fx-background-color: #FFFFFF;");
    return content;
  }

  private ImageView createImagePreview(String imageUrl) {
    ImageView preview = new ImageView();
    preview.setFitWidth(220);
    preview.setFitHeight(145);
    preview.setPreserveRatio(true);
    if (imageUrl != null && !imageUrl.isBlank()) {
      preview.setImage(new Image(imageUrl, 220, 145, true, true, true));
    }
    return preview;
  }

  private HBox createImagePicker(
      TextField txtImageUrl,
      ImageView imagePreview,
      File[] selectedImageFile
  ) {
    Button chooseButton = new Button("Chọn ảnh");
    chooseButton.setStyle("-fx-background-color: #6A4C2B; -fx-text-fill: white; "
        + "-fx-background-radius: 8; -fx-padding: 8 13; -fx-cursor: hand;");
    Label selectedFileLabel = new Label("Chưa chọn ảnh mới");
    selectedFileLabel.setStyle("-fx-text-fill: #6B625A;");
    selectedFileLabel.setMaxWidth(220);
    selectedFileLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
    chooseButton.setOnAction(event -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Chọn ảnh sản phẩm đấu giá");
      fileChooser.getExtensionFilters().add(
          new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
      File selectedFile = fileChooser.showOpenDialog(chooseButton.getScene().getWindow());
      if (selectedFile == null) {
        return;
      }
      selectedImageFile[0] = selectedFile;
      selectedFileLabel.setText(selectedFile.getName());
      txtImageUrl.setText(selectedFile.toURI().toString());
      imagePreview.setImage(new Image(selectedFile.toURI().toString(), 220, 145, true, true, true));
    });

    HBox picker = new HBox(8, chooseButton, selectedFileLabel);
    picker.setAlignment(Pos.CENTER_LEFT);
    return picker;
  }

  private VBox createImageSection(ImageView imagePreview, HBox imagePicker) {
    StackPane previewFrame = new StackPane(imagePreview);
    previewFrame.setMinSize(240, 160);
    previewFrame.setPrefSize(240, 160);
    previewFrame.setStyle("-fx-background-color: #F7F4EF; -fx-border-color: #E4D9CC; "
        + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8;");

    Label title = createSectionTitle("Ảnh sản phẩm");
    Label hint = new Label("Ảnh mới sẽ được tải lên sau khi lưu thay đổi.");
    hint.setStyle("-fx-text-fill: #7B7168; -fx-font-size: 12px;");
    hint.setWrapText(true);

    VBox section = new VBox(8, title, previewFrame, imagePicker, hint);
    section.setAlignment(Pos.CENTER_LEFT);
    return section;
  }

  private Label createSectionTitle(String text) {
    Label label = new Label(text);
    label.setStyle("-fx-text-fill: #3E2723; -fx-font-size: 15px; -fx-font-weight: bold;");
    return label;
  }

  private VBox fieldBlock(String labelText, javafx.scene.Node field) {
    Label label = new Label(labelText);
    label.setStyle("-fx-text-fill: #5D5148; -fx-font-size: 12px; -fx-font-weight: bold;");
    if (field instanceof javafx.scene.control.Control control) {
      control.setMaxWidth(Double.MAX_VALUE);
      control.setStyle(control.getStyle()
          + "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #D9D2CA;");
    }
    VBox box = new VBox(6, label, field);
    HBox.setHgrow(box, Priority.ALWAYS);
    return box;
  }

  private HBox createTwoColumnRow(VBox left, VBox right) {
    HBox row = new HBox(12, left, right);
    row.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(left, Priority.ALWAYS);
    HBox.setHgrow(right, Priority.ALWAYS);
    return row;
  }
}
