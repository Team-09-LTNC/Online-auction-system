package com.auction.client.controller.components;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

        // Load số dư và lịch sử giao dịch ban đầu từ Server về
        fetchWalletHistory();
    }

    private void fetchWalletHistory() {
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("type", "GET_WALLET_HISTORY");

        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "WALLET_HISTORY_RESPONSE", response -> {
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean()) {
                    if (response.has("currentBalance")) {
                        currentBalance = response.get("currentBalance").getAsDouble();
                        lblBalance.setText(String.format("%,.0f đ", currentBalance));
                    }
                    
                    if (response.has("data")) {
                        lvTransactionHistory.getItems().clear();
                        JsonArray historyArray = response.getAsJsonArray("data");
                        for (JsonElement element : historyArray) {
                            JsonObject trans = element.getAsJsonObject();
                            String type = trans.has("type") ? trans.get("type").getAsString() : "";
                            long amount = trans.has("amount") ? trans.get("amount").getAsLong() : 0;
                            String description = trans.has("description") ? trans.get("description").getAsString() : "";
                            String time = trans.has("time") ? trans.get("time").getAsString() : "";
                            
                            String symbol = "🔹";
                            if ("WITHDRAW".equals(type) || "PAYMENT_SENT".equals(type)) symbol = "🔻";
                            
                            String log = String.format("%s [%s] %s: %,d đ - %s", symbol, time, type, amount, description);
                            lvTransactionHistory.getItems().add(log);
                        }
                    }
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
        jsonRequest.addProperty("type", ActionType.TOP_UP_MONEY); // Đồng bộ chuẩn ActionType
        jsonRequest.addProperty("amount", amount);

        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "TOP_UP_RESPONSE", response -> {
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean()) {
                    currentBalance = response.has("newBalance") ? response.get("newBalance").getAsDouble() : (currentBalance + amount);
                    lblBalance.setText(String.format("%,.0f đ", currentBalance));
                    txtAmount.clear();
                    
                    // Tải lại lịch sử từ Server để đồng bộ hoàn toàn
                    fetchWalletHistory();
                } else {
                    String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi hệ thống";
                    Alert alert = new Alert(Alert.AlertType.ERROR, msg);
                    alert.showAndWait();
                }
            });
        });
    }

    @FXML
    private void handleWithdraw(ActionEvent event) {
        String amountText = txtAmount.getText().trim();
        if (amountText.isEmpty()) return;

        long amount = Long.parseLong(amountText);
        if (amount > currentBalance && currentBalance > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Số dư ví không đủ để thực hiện rút tiền!");
            alert.showAndWait();
            return;
        }

        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("type", ActionType.WITHDRAW_MONEY); // Đồng bộ chuẩn ActionType
        jsonRequest.addProperty("amount", amount);

        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "WITHDRAW_RESPONSE", response -> {
            Platform.runLater(() -> {
                if (response.has("success") && response.get("success").getAsBoolean()) {
                    currentBalance = response.has("newBalance") ? response.get("newBalance").getAsDouble() : (currentBalance - amount);
                    lblBalance.setText(String.format("%,.0f đ", currentBalance));
                    txtAmount.clear();
                    
                    // Tải lại lịch sử từ Server để đồng bộ hoàn toàn
                    fetchWalletHistory();
                } else {
                    String msg = response.has("message") ? response.get("message").getAsString() : "Số dư không đủ!";
                    Alert alert = new Alert(Alert.AlertType.WARNING, msg);
                    alert.showAndWait();
                }
            });
        });
    }
}