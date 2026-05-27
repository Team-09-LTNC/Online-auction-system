package com.auction.client.controller.components;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

final class ChatNotificationRenderer {
  private static final int PAYMENT_DEADLINE_HOURS = 24;
  private static final String[] PAYMENT_RULE_LINES = {
      "Quy định:",
      "- Hủy thanh toán hoặc quá hạn thanh toán: hệ thống trừ 10% giá chốt.",
      "- Nếu ví đủ để trừ 10%: không bị khóa tài khoản.",
      "- Nếu ví không đủ: lần 1 khóa 3 ngày, lần 2 khóa 7 ngày.",
      "- Từ lần 3 không đủ phí phạt: khóa tài khoản vĩnh viễn."
  };

  private final List<Timeline> paymentCountdowns = new ArrayList<>();

  Node createRegularNotification(String content, String sentAt) {
    Label title = new Label("Thông báo hệ thống");
    title.setStyle("-fx-text-fill: #7A1B28; -fx-font-size: 13px; -fx-font-weight: bold;");

    Label label = new Label(content);
    label.setWrapText(true);
    label.setMaxWidth(Double.MAX_VALUE);
    label.setStyle("-fx-text-fill: #3E2723; -fx-font-size: 13px; -fx-line-spacing: 2px;");

    VBox card = createNotificationCard();
    card.getChildren().addAll(title, createTimeLabel(sentAt), label);
    return unwrapNotification("!", card);
  }

  Node createPaymentNotification(
      int auctionId,
      String content,
      String sentAt,
      String auctionStatus,
      SettlementAction settlementAction
  ) {
    Label title = new Label("Thông báo chiến thắng");
    title.setStyle("-fx-text-fill: #7A1B28; -fx-font-size: 14px; -fx-font-weight: bold;");

    Label body = new Label(content);
    body.setWrapText(true);
    body.setMaxWidth(Double.MAX_VALUE);
    body.setStyle("-fx-text-fill: #3E2723; -fx-font-size: 13px; -fx-line-spacing: 2px;");

    if ("PAID".equalsIgnoreCase(auctionStatus) || "CANCELED".equalsIgnoreCase(auctionStatus)) {
      Label settled = new Label(
          "PAID".equalsIgnoreCase(auctionStatus)
              ? "Bạn đã thanh toán thành công cho phiên này."
              : "Bạn đã hủy thanh toán cho phiên này.");
      settled.setWrapText(true);
      settled.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1E8449;");

      VBox card = createNotificationCard();
      card.getChildren().addAll(title, createTimeLabel(sentAt), body, settled);
      return unwrapNotification("✓", card);
    }

    Button cancel = createActionButton("Hủy thanh toán", "#F0ECE8", "#3E2723");
    Button confirm = createActionButton("Xác nhận thanh toán", "#B32638", "white");
    Label countdown = createPaymentCountdownLabel(sentAt, cancel, confirm);
    VBox rule = createPaymentRuleBox();
    Label result = createResultLabel();
    HBox actions = new HBox(10, cancel, confirm);
    actions.setAlignment(Pos.CENTER_RIGHT);

    PaymentControls controls = new PaymentControls(cancel, confirm, result, countdown, rule, actions);
    cancel.setOnAction(event -> settlementAction.send(auctionId, "CANCEL", controls));
    confirm.setOnAction(event -> settlementAction.send(auctionId, "CONFIRM", controls));

    VBox card = createNotificationCard();
    card.getChildren().addAll(title, createTimeLabel(sentAt), body, countdown, rule, result, actions);
    return unwrapNotification("✓", card);
  }

  void disableActions(PaymentControls controls) {
    controls.cancel().setDisable(true);
    controls.confirm().setDisable(true);
  }

  void enableActions(PaymentControls controls) {
    controls.cancel().setDisable(false);
    controls.confirm().setDisable(false);
  }

  void showSettlementResult(PaymentControls controls, String message, boolean success) {
    controls.result().setText(message);
    controls.result().setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: "
        + (success ? "#1E8449;" : "#A64452;"));
  }

  void hideSettledPaymentControls(PaymentControls controls) {
    stopPaymentCountdown(controls.countdown());
    hideNode(controls.countdown());
    hideNode(controls.rule());
    hideNode(controls.actions());
  }

  void shutdown() {
    for (Timeline timeline : paymentCountdowns) {
      timeline.stop();
    }
    paymentCountdowns.clear();
  }

  private Button createActionButton(String text, String background, String textColor) {
    Button button = new Button(text);
    button.setStyle("-fx-background-color: " + background + "; -fx-text-fill: " + textColor + "; "
        + "-fx-font-weight: bold; -fx-background-radius: 7; -fx-cursor: hand;");
    return button;
  }

  private Label createResultLabel() {
    Label result = new Label();
    result.setWrapText(true);
    result.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
    return result;
  }

  private VBox createPaymentRuleBox() {
    VBox ruleBox = new VBox(4);
    ruleBox.setMaxWidth(Double.MAX_VALUE);
    ruleBox.setStyle("-fx-background-color: #FFF7E8; -fx-background-radius: 7; "
        + "-fx-border-color: #E7C27D; -fx-border-radius: 7; "
        + "-fx-padding: 8 10;");

    for (int i = 0; i < PAYMENT_RULE_LINES.length; i++) {
      Label line = new Label(PAYMENT_RULE_LINES[i]);
      line.setWrapText(true);
      line.setMaxWidth(Double.MAX_VALUE);
      line.setStyle("-fx-text-fill: #7A4B11; -fx-font-size: 12px; "
          + "-fx-font-weight: bold; -fx-line-spacing: 2px;");
      if (i == 0) {
        line.setStyle(line.getStyle() + "-fx-font-size: 13px;");
      }
      ruleBox.getChildren().add(line);
    }

    return ruleBox;
  }

  private Label createPaymentCountdownLabel(String sentAt, Button cancel, Button confirm) {
    Label countdown = new Label();
    countdown.setWrapText(true);
    countdown.setStyle("-fx-background-color: #F7EAEC; -fx-background-radius: 7; "
        + "-fx-padding: 7 10; -fx-text-fill: #A64452; -fx-font-size: 12px; "
        + "-fx-font-weight: bold;");

    LocalDateTime sentTime = ChatTimeUtil.parseSentAtText(sentAt);
    if (sentTime == LocalDateTime.MIN) {
      sentTime = LocalDateTime.now();
    }
    LocalDateTime deadline = sentTime.plusHours(PAYMENT_DEADLINE_HOURS);
    updatePaymentCountdownLabel(countdown, deadline, cancel, confirm);

    Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event ->
        updatePaymentCountdownLabel(countdown, deadline, cancel, confirm)));
    timeline.setCycleCount(Animation.INDEFINITE);
    timeline.play();
    paymentCountdowns.add(timeline);
    countdown.setUserData(timeline);
    return countdown;
  }

  private void updatePaymentCountdownLabel(
      Label countdown,
      LocalDateTime deadline,
      Button cancel,
      Button confirm
  ) {
    java.time.Duration remaining = java.time.Duration.between(LocalDateTime.now(), deadline);
    if (!remaining.isPositive()) {
      countdown.setText("Đã hết thời hạn thanh toán. Hệ thống sẽ tự động hủy và xử lý phí phạt 10%.");
      countdown.setStyle("-fx-background-color: #F2EEEB; -fx-background-radius: 7; "
          + "-fx-padding: 7 10; -fx-text-fill: #7D6E6A; -fx-font-size: 12px; "
          + "-fx-font-weight: bold;");
      cancel.setDisable(true);
      confirm.setDisable(true);
      stopPaymentCountdown(countdown);
      return;
    }

    long totalSeconds = remaining.getSeconds();
    long hours = totalSeconds / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;
    countdown.setText(String.format(
        "Thời hạn thanh toán còn: %02d:%02d:%02d (hạn cuối %s)",
        hours,
        minutes,
        seconds,
        deadline.format(ChatTimeUtil.DISPLAY_TIME_FORMAT)));
  }

  private Label createTimeLabel(String sentAt) {
    Label time = new Label(ChatTimeUtil.formatTime(sentAt));
    time.setStyle("-fx-text-fill: #8A6F66; -fx-font-size: 11px;");
    return time;
  }

  private VBox createNotificationCard() {
    VBox card = new VBox(10);
    card.setPadding(new Insets(12));
    card.setMaxWidth(Double.MAX_VALUE);
    card.setStyle("-fx-background-color: #FFFDFC; -fx-background-radius: 8; "
        + "-fx-border-color: #E8DDD5; -fx-border-radius: 8;");
    return card;
  }

  private Node unwrapNotification(String icon, VBox content) {
    Label marker = new Label(icon);
    marker.setAlignment(Pos.CENTER);
    marker.setMinSize(30, 30);
    marker.setPrefSize(30, 30);
    marker.setStyle("-fx-background-color: #F4DDE0; -fx-background-radius: 99; "
        + "-fx-text-fill: #A64452; -fx-font-size: 15px; -fx-font-weight: bold;");

    HBox row = new HBox(12, marker, content);
    row.setAlignment(Pos.TOP_LEFT);
    row.setFillHeight(true);
    HBox.setHgrow(content, javafx.scene.layout.Priority.ALWAYS);
    return row;
  }

  private void stopPaymentCountdown(Label countdown) {
    if (countdown == null || !(countdown.getUserData() instanceof Timeline timeline)) {
      return;
    }
    timeline.stop();
    paymentCountdowns.remove(timeline);
    countdown.setUserData(null);
  }

  private void hideNode(Node node) {
    if (node == null) {
      return;
    }
    node.setVisible(false);
    node.setManaged(false);
  }

  @FunctionalInterface
  interface SettlementAction {
    void send(int auctionId, String decision, PaymentControls controls);
  }

  record PaymentControls(
      Button cancel,
      Button confirm,
      Label result,
      Label countdown,
      VBox rule,
      HBox actions
  ) {
  }
}
