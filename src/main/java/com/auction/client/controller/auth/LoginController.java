package com.auction.client.controller.auth;

import com.auction.client.network.NetworkManager;
import com.auction.common.dto.BaseDTOs;
import com.auction.common.enums.ActionType;
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
import java.util.UUID;

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

        if (user.isEmpty() || pass.isEmpty() || role == null) {
            statusLabel.setText("Vui lòng nhập đủ thông tin và chọn vai trò!");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        // --- CHUẨN KIẾN TRÚC: Đẩy việc giao tiếp mạng cho Network Layer ---
        BaseDTOs.Request loginReq = new BaseDTOs.Request();
        loginReq.type = ActionType.LOGIN;
        loginReq.requestId = UUID.randomUUID().toString();

        // Gửi yêu cầu qua đường ống duy nhất, không mở socket mới ở đây
        NetworkManager.getInstance().sendRequest(loginReq);

        statusLabel.setText("Đang xác thực...");
        statusLabel.setStyle("-fx-text-fill: #3498db;");

        navigateToHome(event);
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
        switchScene(event, "/fxml/bidder/MainLayout.fxml", "Trang chủ Đấu giá");
    }

    @FXML
    private void onRegisterLinkClick(ActionEvent event) {
        switchScene(event, "/fxml/auth/Register.fxml", "Đăng ký tài khoản");
    }

    /**
     * Hàm chuyển giao diện bằng cách thay đổi Root của Scene hiện tại.
     * Giúp giữ nguyên kích thước cửa sổ (Stage), không bị lỗi co màn hình (Shrink).
     */
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            URL fxmlLocation = getClass().getResource(fxmlPath);
            if (fxmlLocation == null) {
                throw new IOException("Không tìm thấy file FXML tại: " + fxmlPath);
            }

            // Tải nội dung mới từ file FXML
            Parent newRoot = FXMLLoader.load(fxmlLocation);

            // Lấy Scene hiện tại từ nút bấm (event source)
            Scene currentScene = ((Node) event.getSource()).getScene();

            // CHỖ QUAN TRỌNG: Thay đổi nội dung gốc (Root) thay vì tạo Scene mới
            currentScene.setRoot(newRoot);

            // Cập nhật lại tiêu đề cửa sổ cho đúng trang
            Stage stage = (Stage) currentScene.getWindow();
            stage.setTitle(title);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi Hệ thống", "Không thể tải giao diện: " + e.getMessage());
        }
    }

    private void navigateToHome(ActionEvent event) {
        String role = roleComboBox.getValue();
        if ("Admin".equalsIgnoreCase(role)) {
            switchScene(event, "/fxml/admin/AdminLayout.fxml", "Admin Dashboard");
        } else {
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