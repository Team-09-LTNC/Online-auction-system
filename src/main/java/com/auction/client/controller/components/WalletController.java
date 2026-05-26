package com.auction.client.controller.components;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

public class WalletController {

    @FXML private Label lblBalance;
    @FXML private TextField txtAmount;
    @FXML private ListView<String> lvTransactionHistory;

    private long currentBalance = 0L;

    @FXML
    public void initialize() {
        installMoneyFormatter(txtAmount);

        fetchWalletHistory();
    }

    private void installMoneyFormatter(TextField textField) {
        if (textField == null) {
            return;
        }

        textField.setTextFormatter(new TextFormatter<String>(change -> {
            String proposedText = change.getControlNewText();
            String digits = getDigitsOnly(proposedText);
            String formatted = formatDigitsWithDots(digits);
            int caretPosition = change.getCaretPosition();
            int digitsBeforeCaret = getDigitsOnly(proposedText.substring(0, Math.min(
                    caretPosition,
                    proposedText.length()
            ))).length();

            change.setRange(0, change.getControlText().length());
            change.setText(formatted);
            int newCaret = calculateCaretPosition(formatted, digitsBeforeCaret);
            change.setCaretPosition(newCaret);
            change.setAnchor(newCaret);
            return change;
        }));
    }

    private void fetchWalletHistory() {
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("type", "GET_WALLET_HISTORY");
        jsonRequest.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "WALLET_HISTORY_RESPONSE", response ->
                Platform.runLater(() -> {
                    if (!(response.has("success") && response.get("success").getAsBoolean())) {
                        return;
                    }

                    if (response.has("currentBalance")) {
                        currentBalance = response.get("currentBalance").getAsLong();
                        lblBalance.setText(formatVnd(currentBalance));
                    }

                    if (!response.has("data")) {
                        return;
                    }

                    lvTransactionHistory.getItems().clear();
                    JsonArray historyArray = response.getAsJsonArray("data");
                    for (JsonElement element : historyArray) {
                        JsonObject trans = element.getAsJsonObject();
                        String type = trans.has("type") ? trans.get("type").getAsString() : "";
                        long amount = trans.has("amount") ? trans.get("amount").getAsLong() : 0;
                        String description = trans.has("description") ? trans.get("description").getAsString() : "";
                        String time = trans.has("time") ? trans.get("time").getAsString() : "";

                        String symbol = "🔹";
                        if ("WITHDRAW".equals(type) || "PAYMENT_SENT".equals(type)) {
                            symbol = "🔻";
                        }

                        String log = String.format(
                                "%s [%s] %s: %s - %s",
                                symbol,
                                time,
                                type,
                                formatVnd(amount),
                                description
                        );
                        lvTransactionHistory.getItems().add(log);
                    }
                }));
    }

    @FXML
    private void handleDeposit(ActionEvent event) {
        String amountText = txtAmount.getText().trim();
        if (amountText.isEmpty()) {
            return;
        }

        Long amount = parseValidAmount(amountText);
        if (amount == null) {
            return;
        }

        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("type", ActionType.TOP_UP_MONEY);
        jsonRequest.addProperty("amount", amount);
        jsonRequest.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "TOP_UP_RESPONSE", response ->
                Platform.runLater(() -> {
                    if (response.has("success") && response.get("success").getAsBoolean()) {
                        currentBalance = response.has("newBalance")
                                ? response.get("newBalance").getAsLong()
                                : (currentBalance + amount);
                        lblBalance.setText(formatVnd(currentBalance));
                        txtAmount.clear();
                        fetchWalletHistory();
                    } else {
                        String msg = response.has("message")
                                ? response.get("message").getAsString()
                                : "Lỗi hệ thống";
                        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
                    }
                }));
    }

    @FXML
    private void handleWithdraw(ActionEvent event) {
        String amountText = txtAmount.getText().trim();
        if (amountText.isEmpty()) {
            return;
        }

        Long amount = parseValidAmount(amountText);
        if (amount == null) {
            return;
        }

        if (amount > currentBalance && currentBalance > 0) {
            new Alert(Alert.AlertType.WARNING, "Số dư ví không đủ để thực hiện rút tiền!").showAndWait();
            return;
        }

        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("type", ActionType.WITHDRAW_MONEY);
        jsonRequest.addProperty("amount", amount);
        jsonRequest.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "WITHDRAW_RESPONSE", response ->
                Platform.runLater(() -> {
                    if (response.has("success") && response.get("success").getAsBoolean()) {
                        currentBalance = response.has("newBalance")
                                ? response.get("newBalance").getAsLong()
                                : (currentBalance - amount);
                        lblBalance.setText(formatVnd(currentBalance));
                        txtAmount.clear();
                        fetchWalletHistory();
                    } else {
                        String msg = response.has("message")
                                ? response.get("message").getAsString()
                                : "Số dư không đủ!";
                        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
                    }
                }));
    }

    private Long parseValidAmount(String amountText) {
        try {
            long amount = Long.parseLong(getDigitsOnly(amountText));
            if (amount <= 0) {
                new Alert(Alert.AlertType.WARNING, "Số tiền phải lớn hơn 0.").showAndWait();
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            new Alert(
                    Alert.AlertType.WARNING,
                    "Số tiền quá lớn hoặc không hợp lệ. Vui lòng nhập nhỏ hơn "
                            + formatDigitsWithDots(String.valueOf(Long.MAX_VALUE)) + "."
            ).showAndWait();
            return null;
        }
    }

    private String formatVnd(long value) {
        return formatDigitsWithDots(String.valueOf(value)) + " đ";
    }

    private String formatDigitsWithDots(String digits) {
        if (digits == null || digits.isBlank()) {
            return "";
        }

        String normalized = digits.replaceFirst("^0+(?!$)", "");
        StringBuilder formatted = new StringBuilder();
        int firstGroupLength = normalized.length() % 3;
        if (firstGroupLength == 0) {
            firstGroupLength = 3;
        }

        formatted.append(normalized, 0, firstGroupLength);
        for (int i = firstGroupLength; i < normalized.length(); i += 3) {
            formatted.append('.').append(normalized, i, i + 3);
        }
        return formatted.toString();
    }

    private String getDigitsOnly(String text) {
        return text == null ? "" : text.replaceAll("\\D", "");
    }

    private int calculateCaretPosition(String formatted, int digitsBeforeCaret) {
        if (digitsBeforeCaret <= 0) {
            return 0;
        }

        int seenDigits = 0;
        for (int i = 0; i < formatted.length(); i++) {
            if (Character.isDigit(formatted.charAt(i))) {
                seenDigits++;
            }
            if (seenDigits >= digitsBeforeCaret) {
                return i + 1;
            }
        }
        return formatted.length();
    }
}
