package com.auction.client.controller.components;

import com.auction.client.networkclient.ClientSocket;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class WalletController {

    @FXML private Label lblBalance;
    @FXML private TextField txtAmount;
    @FXML private ListView<String> lvTransactionHistory;

    private double currentBalance = 0.0;

    @FXML
    public void initialize() {
        // Tắt chữ nếu nhập chữ vào ô tiền
        txtAmount.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                txtAmount.setText(newVal.replaceAll("\\D", ""));
            }
        });

        // Load số dư ban đầu từ Server về
        fetchWalletBalance();
    }

    private void fetchWalletBalance() {
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("type", "GET_WALLET_BALANCE");

        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "WALLET_BALANCE_RESPONSE", response -> {
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean()) {
                    currentBalance = response.get("balance").getAsDouble();
                    lblBalance.setText(String.format("%,.0f đ", currentBalance));
                }
            });
        });
    }

    @FXML
    private void handleDeposit(ActionEvent event) {
        String amountText = txtAmount.getText().trim();
        if (amountText.isEmpty()) return;

        long amount = Long.parseLong(amountText);

        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("type", "WALLET_DEPOSIT");
        jsonRequest.addProperty("amount", amount);

        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "WALLET_TRANSACTION_RESPONSE", response -> {
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean()) {
                    currentBalance += amount;
                    lblBalance.setText(String.format("%,.0f đ", currentBalance));
                    lvTransactionHistory.getItems().add(0, String.format("➕ Nạp tiền: +%,d đ (Thành công)", amount));
                    txtAmount.clear();
                }
            });
        });
    }

    @FXML
    private void handleWithdraw(ActionEvent event) {
        String amountText = txtAmount.getText().trim();
        if (amountText.isEmpty()) return;

        long amount = Long.parseLong(amountText);
        if (amount > currentBalance) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Số dư ví không đủ để thực hiện rút tiền!");
            alert.showAndWait();
            return;
        }

        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("type", "WALLET_WITHDRAW");
        jsonRequest.addProperty("amount", amount);

        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "WALLET_TRANSACTION_RESPONSE", response -> {
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean()) {
                    currentBalance -= amount;
                    lblBalance.setText(String.format("%,.0f đ", currentBalance));
                    lvTransactionHistory.getItems().add(0, String.format("➖ Rút tiền: -%,d đ (Thành công)", amount));
                    txtAmount.clear();
                }
            });
        });
    }
}