package com.auction.client.controller.components;

import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

final class ChatTimeUtil {
  static final DateTimeFormatter DISPLAY_TIME_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final DateTimeFormatter DB_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private ChatTimeUtil() {
  }

  static String formatTime(String sentAt) {
    LocalDateTime parsed = parseSentAtText(sentAt);
    if (parsed == LocalDateTime.MIN) {
      return LocalDateTime.now().format(DISPLAY_TIME_FORMAT);
    }
    return parsed.format(DISPLAY_TIME_FORMAT);
  }

  static LocalDateTime parseSentAt(JsonObject payload) {
    if (payload == null || !payload.has("sentAt") || payload.get("sentAt").isJsonNull()) {
      return LocalDateTime.MIN;
    }
    return parseSentAtText(payload.get("sentAt").getAsString());
  }

  static LocalDateTime parseSentAtForOrder(String sentAt) {
    LocalDateTime parsed = parseSentAtText(sentAt);
    return LocalDateTime.MIN.equals(parsed) ? LocalDateTime.now() : parsed;
  }

  static LocalDateTime parseSentAtText(String sentAt) {
    if (sentAt == null || sentAt.isBlank()) {
      return LocalDateTime.MIN;
    }

    String normalized = sentAt.trim().replace('T', ' ');
    int dotIndex = normalized.indexOf('.');
    if (dotIndex > 0) {
      normalized = normalized.substring(0, dotIndex);
    }

    try {
      return LocalDateTime.parse(normalized, DB_TIME_FORMAT);
    } catch (DateTimeParseException ignored) {
      return LocalDateTime.MIN;
    }
  }
}
