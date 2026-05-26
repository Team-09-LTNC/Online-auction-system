package com.auction.client.controller.components;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

final class ProductCardStatusView {
  static final String ACTION_BUTTON_LIVE = "auction-action-live";
  static final String ACTION_BUTTON_WAITING = "auction-action-waiting";
  static final String ACTION_BUTTON_EXPIRED = "auction-action-expired";
  static final String ACTION_BUTTON_PAID = "auction-action-paid";
  static final String ACTION_BUTTON_CANCELED = "auction-action-canceled";
  static final String STATUS_LIVE = "status-live";
  static final String STATUS_WAITING = "status-waiting";
  static final String STATUS_EXPIRED = "status-expired";
  static final String STATUS_PAID = "status-paid";
  static final String STATUS_CANCELED = "status-canceled";

  private static final String ACTION_BUTTON_BASE = "auction-action-button";

  private ProductCardStatusView() {
  }

  static boolean isTimedStatus(String status) {
    return "OPEN".equalsIgnoreCase(status) || "RUNNING".equalsIgnoreCase(status);
  }

  static String formatTime(int totalSeconds) {
    int h = totalSeconds / 3600;
    int m = (totalSeconds % 3600) / 60;
    int s = totalSeconds % 60;
    return String.format("%02d:%02d:%02d", h, m, s);
  }

  static void setStatusBadge(Label statusLabel, String text, String variantStyleClass) {
    statusLabel.getStyleClass().removeAll(
        STATUS_LIVE,
        STATUS_WAITING,
        STATUS_EXPIRED,
        STATUS_PAID,
        STATUS_CANCELED
    );
    statusLabel.setText(text);
    if (variantStyleClass != null && !statusLabel.getStyleClass().contains(variantStyleClass)) {
      statusLabel.getStyleClass().add(variantStyleClass);
    }
  }

  static void setBidButtonState(Button bidButton, String text, boolean disabled, String variantStyleClass) {
    bidButton.getStyleClass().removeAll(
        "btn-primary",
        ACTION_BUTTON_LIVE,
        ACTION_BUTTON_WAITING,
        ACTION_BUTTON_EXPIRED,
        ACTION_BUTTON_PAID,
        ACTION_BUTTON_CANCELED
    );
    if (!bidButton.getStyleClass().contains(ACTION_BUTTON_BASE)) {
      bidButton.getStyleClass().add(ACTION_BUTTON_BASE);
    }
    if (variantStyleClass != null) {
      bidButton.getStyleClass().add(variantStyleClass);
    }
    bidButton.setText(text);
    bidButton.setDisable(disabled);
  }
}
