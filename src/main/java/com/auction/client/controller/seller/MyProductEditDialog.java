package com.auction.client.controller.seller;

import com.google.gson.JsonObject;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

final class MyProductEditDialog {
  Optional<MyProductEditRequest> show(JsonObject itemObj) {
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Sửa sản phẩm");
    dialog.setHeaderText("Chỉ có thể sửa khi phiên đang chờ mở.");
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    TextField txtName = new TextField(MyProductsHelper.getString(itemObj, "name", ""));
    TextArea txtDescription = new TextArea(MyProductsHelper.getString(itemObj, "description", ""));
    txtDescription.setPrefRowCount(3);
    TextField txtPrice = new TextField(String.valueOf(MyProductsHelper.getLong(itemObj, "startingPrice", 0L)));
    ComboBox<String> categoryBox = new ComboBox<>();
    categoryBox.getItems().setAll("ELECTRONICS", "VEHICLE", "ART", "OTHER");
    categoryBox.setValue(MyProductsHelper.getString(itemObj, "category", "OTHER"));
    TextField txtImageUrl = new TextField(MyProductsHelper.getString(itemObj, "imageUrl", ""));
    DatePicker dpStartDate = new DatePicker();
    TextField txtStartTime = new TextField();
    DatePicker dpEndDate = new DatePicker();
    TextField txtEndTime = new TextField();
    MyProductsHelper.fillDateTimeFields(
        MyProductsHelper.getString(itemObj, "startTime", null), dpStartDate, txtStartTime);
    MyProductsHelper.fillDateTimeFields(
        MyProductsHelper.getString(itemObj, "endTime", null), dpEndDate, txtEndTime);

    dialog.getDialogPane().setContent(createForm(
        txtName, txtDescription, txtPrice, categoryBox, txtImageUrl,
        dpStartDate, txtStartTime, dpEndDate, txtEndTime));

    return dialog.showAndWait()
        .filter(button -> button == ButtonType.OK)
        .map(button -> new MyProductEditRequest(
            txtName.getText(),
            txtDescription.getText(),
            txtPrice.getText(),
            categoryBox.getValue(),
            txtImageUrl.getText(),
            dpStartDate.getValue(),
            txtStartTime.getText(),
            dpEndDate.getValue(),
            txtEndTime.getText()));
  }

  private GridPane createForm(
      TextField txtName,
      TextArea txtDescription,
      TextField txtPrice,
      ComboBox<String> categoryBox,
      TextField txtImageUrl,
      DatePicker dpStartDate,
      TextField txtStartTime,
      DatePicker dpEndDate,
      TextField txtEndTime
  ) {
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(10));
    grid.addRow(0, new Label("Tên"), txtName);
    grid.addRow(1, new Label("Mô tả"), txtDescription);
    grid.addRow(2, new Label("Giá khởi điểm"), txtPrice);
    grid.addRow(3, new Label("Loại"), categoryBox);
    grid.addRow(4, new Label("Ảnh"), txtImageUrl);
    grid.addRow(5, new Label("Ngày bắt đầu"), dpStartDate);
    grid.addRow(6, new Label("Giờ bắt đầu"), txtStartTime);
    grid.addRow(7, new Label("Ngày kết thúc"), dpEndDate);
    grid.addRow(8, new Label("Giờ kết thúc"), txtEndTime);
    return grid;
  }
}
