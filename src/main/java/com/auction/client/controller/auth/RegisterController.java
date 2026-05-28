package com.auction.client.controller.auth;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.dto.AuthDTOs;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class RegisterController {

    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordTextField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label statusLabel;
    @FXML private Hyperlink loginLink;

    @FXML
    public void initialize() {
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("Bidder", "Seller");
        }
    }

    @FXML
    void onRegisterButtonClick(ActionEvent event) {
        String fullName = fullNameField.getText().trim();
        String user = usernameField.getText().trim();

        String pass = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();
        String confirm = confirmPasswordField.isVisible() ? confirmPasswordField.getText() : confirmPasswordTextField.getText();
        String role = roleComboBox.getValue();

        // 1. Kiểm tra dữ liệu trống
        if (fullName.isEmpty() || user.isEmpty() || pass.isEmpty() || role == null) {
            updateStatus("Thiếu thông tin!", "red");
            return;
        }

        // 2. Kiểm tra xác nhận mật khẩu
        if (!pass.equals(confirm)) {
            updateStatus("Mật khẩu xác nhận không khớp!", "red");
            return;
        }

        sendDataToServer(fullName, user, pass, role, event);
    }

    /**
     * Xử lý giao tiếp mạng để đăng ký tài khoản
     */
    private void sendDataToServer(String fullName, String user, String pass, String role, ActionEvent event) {
        try {
            // 3. Khởi tạo đối tượng DTO
            AuthDTOs.RegisterRequest regReq = new AuthDTOs.RegisterRequest(user, pass, fullName, role);
            updateStatus("Đang gửi yêu cầu đăng ký...", "blue");

            //4. Ép kiểu sang JsonObject 
            JsonObject jsonRequest = new Gson().toJsonTree(regReq).getAsJsonObject();
            jsonRequest.addProperty("requestId", java.util.UUID.randomUUID().toString());

            // 5. Gửi JSON qua socket và định tuyến callback theo "REGISTER_RESPONSE"
            ClientSocket.getInstance().sendJsonRequest(jsonRequest, "REGISTER_RESPONSE", responseJson -> {
                Platform.runLater(() -> {
                    boolean success = responseJson.has("success") && responseJson.get("success").getAsBoolean();
                    if (success) {
                        updateStatus("Đăng ký thành công!", "green");
                        onLoginLinkClick(event); 
                    } else {
                        String msg = responseJson.has("message") ? responseJson.get("message").getAsString() : "Đăng ký thất bại!";
                        updateStatus(msg, "red");
                    }
                });
            });

        } catch (Exception e) {
            updateStatus("Lỗi kết nối Network!", "red");
        }
    }

    @FXML
    void onShowPasswordButtonClick(ActionEvent event) {
        togglePassword(passwordField, passwordTextField);
    }

    @FXML
    void onShowConfirmPasswordButtonClick(ActionEvent event) {
        togglePassword(confirmPasswordField, confirmPasswordTextField);
    }

    /**
     * Xử lý dùng chung để ẩn/hiện mật khẩu
     */
    private void togglePassword(PasswordField pf, TextField tf) {
        if (pf.isVisible()) {
            tf.setText(pf.getText());
            tf.setVisible(true);
            pf.setVisible(false);
        } else {
            pf.setText(tf.getText());
            pf.setVisible(true);
            tf.setVisible(false);
        }
    }

    @FXML
    void onLoginLinkClick(ActionEvent event) {
        switchScene(event, "/fxml/auth/Login.fxml", "Đăng nhập tài khoản");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            URL fxmlLocation = getClass().getResource(fxmlPath);
            if (fxmlLocation == null) {
                throw new IOException("Không tìm thấy file FXML tại: " + fxmlPath);
            }
            Parent newRoot = FXMLLoader.load(fxmlLocation);
            Scene currentScene = ((Node) event.getSource()).getScene();
            currentScene.setRoot(newRoot);

            Stage stage = (Stage) currentScene.getWindow();
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            updateStatus("Lỗi tải giao diện!", "red");
        }
    }

    private void updateStatus(String message, String color) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + color + ";");
    }
}
