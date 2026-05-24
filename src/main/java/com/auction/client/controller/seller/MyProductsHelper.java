package com.auction.client.controller.seller;

import com.auction.client.util.AuctionTimeUtil;
import com.google.gson.JsonObject;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class MyProductsHelper {
    private MyProductsHelper() {
    }

    static void fillDateTimeFields(String rawValue, DatePicker datePicker, TextField timeField) {
        LocalDateTime value = AuctionTimeUtil.parse(rawValue);
        if (value == null) {
            value = LocalDateTime.now().plusMinutes(10);
        }
        datePicker.setValue(value.toLocalDate());
        timeField.setText(value.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeField.setPromptText("HH:mm");
    }

    static LocalDateTime parseDateTime(LocalDate date, String timeText) {
        if (date == null) {
            return null;
        }
        String normalizedTime = timeText == null ? "" : timeText.trim();
        if (normalizedTime.isEmpty()) {
            normalizedTime = "00:00";
        }
        try {
            LocalTime time = LocalTime.parse(normalizedTime, DateTimeFormatter.ofPattern("HH:mm"));
            return LocalDateTime.of(date, time);
        } catch (Exception e) {
            return null;
        }
    }

    static String resolveDisplayStatus(String storedStatus, String timeStatus) {
        if (storedStatus == null || storedStatus.isBlank()) {
            return timeStatus;
        }
        String normalized = storedStatus.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "PAID":
            case "CANCELED":
            case "FINISHED":
                return normalized;
            case "OPEN":
            case "RUNNING":
                return timeStatus;
            default:
                return normalized;
        }
    }

    static String getString(JsonObject obj, String key, String fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : fallback;
    }

    static int getInt(JsonObject obj, String key, int fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : fallback;
    }

    static long getLong(JsonObject obj, String key, long fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsLong() : fallback;
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
