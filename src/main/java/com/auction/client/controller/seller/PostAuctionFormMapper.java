package com.auction.client.controller.seller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

final class PostAuctionFormMapper {
  private PostAuctionFormMapper() {
  }

  static long parseMoney(String value) {
    return Long.parseLong(value.replaceAll("[^\\d]", ""));
  }

  static long parseOptionalMoney(TextField field) {
    if (field == null || field.getText() == null || field.getText().isEmpty()) {
      return 0L;
    }
    return parseMoney(field.getText());
  }

  static String mapCategoryToEnum(String uiCategory) {
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

  static LocalDateTime parseDateTime(DatePicker datePicker, TextField timeField) throws DateTimeParseException {
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
}
