package com.auction.client.controller.auth;

import com.auction.client.networkclient.ClientSocket;
import com.auction.client.controller.components.SidebarController;
import com.auction.common.dto.AuthDTOs;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;


public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label statusLabel;
    @FXML private Button showPasswordButton;

    @FXML
    public void initialize() {
        establishSocketConnection();

        if (roleComboBox != null) {
            roleComboBox.setItems(FXCollections.observableArrayList("BIDDER", "SELLER", "ADMIN"));
            roleComboBox.setConverter(new StringConverter<String>() {
                @Override
                public String toString(String object) {
                    if (object == null) return "";
                    switch (object) {
                        case "BIDDER": return "Bidder";
                        case "SELLER": return "Seller";
                        case "ADMIN":  return "Admin";
                        default: return object;
                    }
                }
                @Override
                public String fromString(String string) { return string; }
            });
        }
    }

    private void establishSocketConnection() {
        com.auction.client.util.ClientTaskExecutor.execute(() -> {
            try {
                ClientSocket clientSocket = ClientSocket.getInstance();
                if (clientSocket != null) {
                    org.slf4j.LoggerFactory.getLogger(getClass()).info("[Socket] Đường truyền Socket đã sẵn sàng phục vụ đăng nhập!");
                }
            } catch (Throwable t) {
                org.slf4j.LoggerFactory.getLogger(getClass()).error("[Socket Error] Không thể thông luồng mạng: {}", t.getMessage());
            }
        });
    }

    @FXML
    private void onLogInButtonClick(ActionEvent event) {
        String user = usernameField.getText();
        String pass = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();
        String role = (roleComboBox.getValue() != null) ? roleComboBox.getValue() : null;

        if (user.isEmpty() || pass.isEmpty() || role == null) {
            statusLabel.setText("Vui lòng nhập đủ thông tin và chọn vai trò!");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        statusLabel.setText("Đang xác thực...");
        statusLabel.setStyle("-fx-text-fill: #3498db;");

        AuthDTOs.LoginRequest loginReq = new AuthDTOs.LoginRequest(user, pass, role);
        JsonObject jsonRequest = new Gson().toJsonTree(loginReq).getAsJsonObject();
        jsonRequest.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "LOGIN_RESPONSE", responseJson -> {
            Platform.runLater(() -> {
                boolean success = responseJson.has("success") && responseJson.get("success").getAsBoolean();
                if (success) {
                    UserSession.setCurrentRole(role);

                    if (responseJson.has("userData")) {
                        JsonObject userData = responseJson.getAsJsonObject("userData");

                        // ĐỒNG BỘ QUAN TRỌNG: Lấy ID người dùng thực từ server trả về để gán vào phiên client
                        if (userData.has("id")) {
                            UserSession.setUserId(userData.get("id").getAsInt());
                        }

                        String userName = userData.has("username") ? userData.get("username").getAsString() : user;
                        UserSession.setUsername(userName);
                    }

                    com.auction.client.util.AuctionWarmupCache.warmAfterLogin(role);
                    navigateToHome(role);

                    if (SidebarController.instance != null) {
                        SidebarController.instance.applyRolePermissions();
                    }
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
    private void onBackToHomeClick(ActionEvent event) { switchScene("/fxml/bidder/MainLayout.fxml", "Trang chủ Đấu giá"); }

    @FXML
    private void onRegisterLinkClick(ActionEvent event) { switchScene("/fxml/auth/Register.fxml", "Đăng ký tài khoản"); }

    private void switchScene(String fxmlPath, String title) {
        try {
            Parent newRoot = com.auction.client.util.ViewCacheManager.getView(fxmlPath);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            double currentWidth = stage.getScene().getWidth();
            double currentHeight = stage.getScene().getHeight();
            if (newRoot.getScene() != null) {
                stage.setScene(newRoot.getScene());
            } else {
                stage.setScene(new Scene(newRoot, currentWidth, currentHeight));
            }

            stage.setTitle(title);
            stage.show();

        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Lỗi chuyển màn hình: ", e);
            showAlert("Lỗi Hệ thống", "Không thể tải giao diện: " + e.getMessage());
        }
    }

    private void navigateToHome(String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            switchScene("/fxml/admin/AdminLayout.fxml", "Admin Dashboard");
        } else {
            switchScene("/fxml/bidder/MainLayout.fxml", "Client Dashboard");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
