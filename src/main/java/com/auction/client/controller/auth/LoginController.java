package com.auction.client.controller.auth;

import com.auction.client.networkclient.ClientSocket;
import com.auction.client.controller.components.SidebarController; // 🔥 IMPORT ĐỂ ĐIỀU KHIỂN SIDEBAR TỪ XA
import com.auction.common.dto.AuthDTOs;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;

    // Chuẩn hóa Generic <String> để đảm bảo Type-safe
    @FXML private ComboBox<String> roleComboBox;

    @FXML private Label statusLabel;
    @FXML private Button showPasswordButton;

    @FXML
    public void initialize() {
        // Tự động kết nối Socket chạy ngầm ngay khi màn hình Login xuất hiện
        establishSocketConnection();

        if (roleComboBox != null) {
            roleComboBox.setItems(FXCollections.observableArrayList("BIDDER", "SELLER", "ADMIN"));

            // Đã sửa thành setConverter chuẩn JavaFX, hiển thị chữ đẹp mắt
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
                public String fromString(String string) {
                    return string;
                }
            });

            // 🔥 ĐÃ SỬA: Xóa bỏ hoàn toàn dòng selectFirst() để ComboBox
            // hiện chữ mờ mặc định của FXML, bắt người dùng phải bấm vào chọn vai trò!
        }
    }

    private void establishSocketConnection() {
        new Thread(() -> {
            try {
                ClientSocket clientSocket = ClientSocket.getInstance();
                if (clientSocket != null) {
                    org.slf4j.LoggerFactory.getLogger(getClass()).info("[Socket] Đường truyền Socket đã sẵn sàng phục vụ đăng nhập!");
                }
            } catch (Throwable t) {
                org.slf4j.LoggerFactory.getLogger(getClass()).error("[Socket Error] Không thể thông luồng mạng: {}", t.getMessage());
            }
        }).start();
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

        AuthDTOs.LoginRequest loginReq = new AuthDTOs.LoginRequest(user, pass);
        JsonObject jsonRequest = new Gson().toJsonTree(loginReq).getAsJsonObject();

        // Gửi yêu cầu đăng nhập và xử lý bất đồng bộ kết quả trả về từ Server
        ClientSocket.getInstance().sendJsonRequest(jsonRequest, "LOGIN_RESPONSE", responseJson -> {
            Platform.runLater(() -> {
                boolean success = responseJson.has("success") && responseJson.get("success").getAsBoolean();
                if (success) {
                    // 1. Lưu vai trò của người dùng vào Session chuẩn chỉ
                    UserSession.setCurrentRole(role);

                    // 2. Chuyển màn hình giao diện chính (Dashboard) lên trước
                    navigateToHome(role);

                    // 3. 🔥 CHÌA KHÓA VÀNG: Gọi từ xa ép Sidebar phải quét lại quyền, dồn dòng menu khít rịt theo vai trò thật!
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
    private void onBackToHomeClick(ActionEvent event) {
        switchScene("/fxml/bidder/MainLayout.fxml", "Trang chủ Đấu giá");
    }

    @FXML
    private void onRegisterLinkClick(ActionEvent event) {
        switchScene("/fxml/auth/Register.fxml", "Đăng ký tài khoản");
    }

    /**
     * Chuyển đổi Scene an toàn không phụ thuộc ActionEvent bừa bãi
     */
    private void switchScene(String fxmlPath, String title) {
        try {
            URL fxmlLocation = getClass().getResource(fxmlPath);
            if (fxmlLocation == null) {
                throw new IOException("Không tìm thấy file FXML tại: " + fxmlPath);
            }

            Parent newRoot = FXMLLoader.load(fxmlLocation);

            // Lấy Window/Stage trực tiếp từ node giao diện hiện tại cực kỳ an toàn
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene currentScene = stage.getScene();

            currentScene.setRoot(newRoot);
            stage.setTitle(title);
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Lỗi chuyển màn hình: ", e);
            showAlert("Lỗi Hệ thống", "Không thể tải giao diện: " + e.getMessage());
        }
    }

    /**
     * Định tuyến người dùng dựa trên vai trò
     */
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