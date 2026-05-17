package com.auction.client.controller.auth;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.dto.AuthDTOs;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label statusLabel;
    @FXML private Button showPasswordButton;

    @FXML
    public void initialize() {
        if (roleComboBox != null) {
            roleComboBox.setItems(FXCollections.observableArrayList("Bidder", "Seller", "Admin"));
        }
    }

    @FXML
    private void onLogInButtonClick(ActionEvent event) {
        String user = usernameField.getText();
        String pass = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();
        String role = roleComboBox.getValue();

        // 1. Kiểm tra tính hợp lệ của dữ liệu đầu vào
        if (user.isEmpty() || pass.isEmpty() || role == null) {
            statusLabel.setText("Vui lòng nhập đủ thông tin và chọn vai trò!");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        statusLabel.setText("Đang xác thực...");
        statusLabel.setStyle("-fx-text-fill: #3498db;");

        // 2. Khởi tạo đối tượng DTO chuẩn
        AuthDTOs.LoginRequest loginReq = new AuthDTOs.LoginRequest(user, pass);

        // [KIẾN TRÚC MỚI] 3. Chuyển đổi DTO thành JsonObject để hàm mạng gán requestId
        JsonObject jsonRequest = new Gson().toJsonTree(loginReq).getAsJsonObject();

        // 4. Gửi JSON qua Socket và đăng ký Callback chờ "LOGIN_RESPONSE"
        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "LOGIN_RESPONSE", responseJson -> {
            // Đảm bảo thao tác cập nhật UI luôn nằm trên luồng JavaFX (Thread-safety)
            Platform.runLater(() -> {
                boolean success = responseJson.has("success") && responseJson.get("success").getAsBoolean();
                if (success) {
                    navigateToHome(event);
                } else {
                    String msg = responseJson.has("message") ? responseJson.get("message").getAsString() : "Đăng nhập thất bại!";
                    statusLabel.setText(msg);
                    statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                }
            });
        });
    }

    @FXML
    private void onShowPasswordButtonClick(ActionEvent event) {
        // Xử lý logic ẩn/hiện mật khẩu
        if (passwordField.isVisible()) {
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordField.setVisible(false);
            showPasswordButton.setText("🙈");
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordTextField.setVisible(false);
            showPasswordButton.setText("👁");
        }
    }

    @FXML
    private void onBackToHomeClick(ActionEvent event) {
        switchScene(event, "/fxml/bidder/MainLayout.fxml", "Trang chủ Đấu giá");
    }

    @FXML
    private void onRegisterLinkClick(ActionEvent event) {
        switchScene(event, "/fxml/auth/Register.fxml", "Đăng ký tài khoản");
    }

    /**
     * Chuyển đổi Scene (Giao diện) an toàn
     */
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
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi Hệ thống", "Không thể tải giao diện: " + e.getMessage());
        }
    }

    /**
     * Định tuyến người dùng dựa trên vai trò (Role)
     */
    private void navigateToHome(ActionEvent event) {
        String role = roleComboBox.getValue();
        if ("Admin".equalsIgnoreCase(role)) {
            switchScene(event, "/fxml/admin/AdminLayout.fxml", "Admin Dashboard");
        } else {
            // Cả Seller và Bidder tạm dùng chung MainLayout theo thiết kế hiện tại của bạn
            switchScene(event, "/fxml/bidder/MainLayout.fxml", "Client Dashboard");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}