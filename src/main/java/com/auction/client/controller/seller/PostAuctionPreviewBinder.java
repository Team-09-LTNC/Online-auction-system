package com.auction.client.controller.seller;

import com.auction.client.controller.auth.UserSession;
import java.time.format.DateTimeFormatter;
import javafx.beans.InvalidationListener;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

final class PostAuctionPreviewBinder {
  static final int MAX_DESCRIPTION_LENGTH = 3000;

  private PostAuctionPreviewBinder() {
  }

  static void bind(PostAuctionPreviewControls controls) {
    bindProfile(controls);
    bindNameAndCategory(controls);
    bindCurrencyPreview(controls.txtStartingPrice(), controls.lblPreviewPrice());
    bindCurrencyPreview(controls.txtIncrement(), controls.lblPreviewIncrement());
    bindBuyNowPreview(controls.txtBuyNowPrice(), controls.lblPreviewBuyNow());
    bindTimePreview(controls);
    bindAntiSnipingPreview(controls.chkAntiSniping(), controls.lblPreviewAntiSniping());
    bindDescriptionCounter(controls.txtDescription(), controls.lblDescriptionCount());
  }

  static void updateDescriptionCount(Label target, String value) {
    if (target == null) {
      return;
    }
    int count = value == null ? 0 : value.length();
    target.setText(count + "/" + MAX_DESCRIPTION_LENGTH);
    target.setStyle("-fx-text-fill: "
        + (count >= MAX_DESCRIPTION_LENGTH ? "#B32638" : "#A0968C")
        + "; -fx-font-size: 11px; -fx-padding: 0 5 5 0; -fx-font-weight: bold;");
  }

  private static void bindProfile(PostAuctionPreviewControls controls) {
    if (controls.lblProfileName() != null && UserSession.getUsername() != null) {
      controls.lblProfileName().setText("Chào, " + UserSession.getUsername());
    }
    if (controls.lblProfileRole() != null && UserSession.getCurrentRole() != null) {
      String role = UserSession.getCurrentRole();
      controls.lblProfileRole().setText(role.substring(0, 1).toUpperCase() + role.substring(1).toUpperCase());
    }
  }

  private static void bindNameAndCategory(PostAuctionPreviewControls controls) {
    TextField name = controls.txtProductName();
    Label previewName = controls.lblPreviewName();
    if (name != null && previewName != null) {
      name.textProperty().addListener((obs, oldVal, newVal) ->
          previewName.setText(newVal == null || newVal.isEmpty() ? "Tên sản phẩm mẫu..." : newVal));
    }

    ComboBox<String> category = controls.cbCategory();
    Label previewCategory = controls.lblPreviewCategory();
    if (category != null && previewCategory != null) {
      category.valueProperty().addListener((obs, oldVal, newVal) ->
          previewCategory.setText(newVal != null ? newVal : "Chưa chọn"));
    }
  }

  private static void bindDescriptionCounter(TextArea source, Label target) {
    if (source == null || target == null) {
      return;
    }
    updateDescriptionCount(target, source.getText());
    source.textProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null && newVal.length() > MAX_DESCRIPTION_LENGTH) {
        source.setText(newVal.substring(0, MAX_DESCRIPTION_LENGTH));
        return;
      }
      updateDescriptionCount(target, newVal);
    });
  }

  private static void bindCurrencyPreview(TextField source, Label target) {
    if (source == null || target == null) {
      return;
    }
    source.textProperty().addListener((obs, oldVal, newVal) ->
        target.setText(formatCurrencyPreview(newVal, "0 đ")));
  }

  private static void bindBuyNowPreview(TextField source, Label target) {
    if (source == null || target == null) {
      return;
    }
    source.textProperty().addListener((obs, oldVal, newVal) ->
        target.setText(formatCurrencyPreview(newVal, "Không có")));
  }

  private static String formatCurrencyPreview(String rawValue, String emptyText) {
    if (rawValue == null || rawValue.isEmpty()) {
      return emptyText;
    }
    try {
      String clean = rawValue.replaceAll("[^\\d]", "");
      double price = Double.parseDouble(clean);
      return String.format("%,.0f đ", price);
    } catch (NumberFormatException e) {
      return "Giá không hợp lệ";
    }
  }

  private static void bindTimePreview(PostAuctionPreviewControls controls) {
    InvalidationListener listener = obs -> updateTimePreview(controls);
    addDateListener(controls.dpStartDate(), listener);
    addTextListener(controls.txtStartTime(), listener);
    addDateListener(controls.dpEndDate(), listener);
    addTextListener(controls.txtEndTime(), listener);
  }

  private static void updateTimePreview(PostAuctionPreviewControls controls) {
    if (controls.lblPreviewTime() == null) {
      return;
    }
    String startDate = formatDate(controls.dpStartDate());
    String startTime = formatTimeText(controls.txtStartTime());
    String endDate = formatDate(controls.dpEndDate());
    String endTime = formatTimeText(controls.txtEndTime());
    controls.lblPreviewTime().setText(String.format("%s %s\nđến %s %s", startDate, startTime, endDate, endTime));
  }

  private static String formatDate(DatePicker picker) {
    return picker != null && picker.getValue() != null
        ? picker.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        : "??/??/????";
  }

  private static String formatTimeText(TextField field) {
    return field != null && !field.getText().isEmpty() ? field.getText() : "--:--";
  }

  private static void addDateListener(DatePicker picker, InvalidationListener listener) {
    if (picker != null) {
      picker.valueProperty().addListener(listener);
    }
  }

  private static void addTextListener(TextField field, InvalidationListener listener) {
    if (field != null) {
      field.textProperty().addListener(listener);
    }
  }

  private static void bindAntiSnipingPreview(CheckBox source, Label target) {
    if (source == null || target == null) {
      return;
    }
    target.setText(source.isSelected() ? "Có áp dụng" : "Không áp dụng");
    source.selectedProperty().addListener((obs, oldVal, checked) ->
        target.setText(checked ? "Có áp dụng" : "Không áp dụng"));
  }
}
