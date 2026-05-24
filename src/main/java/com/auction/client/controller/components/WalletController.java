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

public class WalletController {

    @FXML private Label lblBalance;
    @FXML private TextField txtAmount;
    @FXML private ListView<String> lvTransactionHistory;

    private double currentBalance = 0.0;

    @FXML
    public void initialize() {
        txtAmount.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                txtAmount.setText(newVal.replaceAll("\\D", ""));
            }
        });

        fetchWalletHistory();
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
                        currentBalance = response.get("currentBalance").getAsDouble();
                        lblBalance.setText(String.format("%,.0f đ", currentBalance));
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

                        String log = String.format("%s [%s] %s: %,d đ - %s", symbol, time, type, amount, description);
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
                                ? response.get("newBalance").getAsDouble()
                                : (currentBalance + amount);
                        lblBalance.setText(String.format("%,.0f đ", currentBalance));
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
                                ? response.get("newBalance").getAsDouble()
                                : (currentBalance - amount);
                        lblBalance.setText(String.format("%,.0f đ", currentBalance));
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
            long amount = Long.parseLong(amountText);
            if (amount <= 0) {
                new Alert(Alert.AlertType.WARNING, "Số tiền phải lớn hơn 0.").showAndWait();
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            new Alert(
                    Alert.AlertType.WARNING,
                    "Số tiền quá lớn hoặc không hợp lệ. Vui lòng nhập nhỏ hơn " + String.format("%,d", Long.MAX_VALUE) + "."
            ).showAndWait();
            return null;
        }
    }
}
