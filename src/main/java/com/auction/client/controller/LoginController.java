package com.auction.client.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.auction.common.exception.AuthenticationException;
import com.auction.common.model.user.User;
import com.auction.server.manager.AuctionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController implements Initializable {

    @FXML private Label welcomeText;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label statusLabel;
    @FXML private TextField passwordTextField;
    @FXML private Button showPasswordButton;
    @FXML private Hyperlink registerLink;

    @FXML
    protected void onShowPasswordButtonClick() {
        if (!passwordTextField.isVisible()) {
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordField.setVisible(false);
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordTextField.setVisible(false);
        }
    }

    @FXML
    protected void onLogInButtonClick() {
        String username = usernameField.getText().trim();
        String password = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();
        String selectedRole = roleComboBox.getValue();

        if (username.isBlank()) {
            showError("Lỗi: Hãy nhập tên đăng nhập của bạn!");
        } else if (password.isBlank()) {
            showError("Lỗi: Vui lòng nhập mật khẩu!");
        } else if (selectedRole == null) {
            showError("Lỗi: Vui lòng chọn vai trò của bạn!");
        } else {
            try {
                // 1. Gọi AuctionManager để xác thực thông tin người dùng
                User user = AuctionManager.getInstance().authenticate(username, password, selectedRole);

                statusLabel.setText("✅ Đăng nhập thành công! Chào " + user.getFullName());
                statusLabel.setStyle("-fx-text-fill: green;");

                // 2. Lấy Stage hiện tại để chuẩn bị chuyển cảnh
                Stage stage = (Stage) statusLabel.getScene().getWindow();
                FXMLLoader loader;

                // 3. Logic điều hướng dựa trên vai trò (Role) [cite: 32, 166]
                if ("Bidder".equals(selectedRole)) {
                    // Chuyển sang màn hình danh sách đấu giá cho Bidder [cite: 377, 378]
                    loader = new FXMLLoader(getClass().getResource("/fxml/AuctionListScreen.fxml"));
                    stage.getScene().setRoot(loader.load());
                    stage.setTitle("HỆ THỐNG ĐẤU GIÁ - DANH SÁCH PHIÊN");
                }
                else if ("Seller".equals(selectedRole)) {
                    // Placeholder cho Seller (Sẽ cập nhật file FXML sau) [cite: 68]
                    showError("Chức năng cho Seller đang được phát triển!");
                }
                else if ("Admin".equals(selectedRole)) {
                    // Placeholder cho Admin (Sẽ cập nhật file FXML sau) [cite: 37]
                    showError("Chức năng cho Admin đang được phát triển!");
                }

            } catch (AuthenticationException e) {
                // Hiển thị thông báo lỗi nếu sai tài khoản/mật khẩu
                showError(e.getMessage());
            } catch (IOException e) {
                // Xử lý lỗi khi không tìm thấy hoặc không load được file FXML
                e.printStackTrace();
                showError("Lỗi hệ thống: Không thể mở giao diện tiếp theo!");
            }
        }
    }

    @FXML
    protected void onRegisterLinkClick() {
        try {
            Stage stage = (Stage) registerLink.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Register.fxml"));
            stage.getScene().setRoot(loader.load());
            stage.setTitle("HỆ THỐNG ĐẤU GIÁ - ĐĂNG KÝ");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Không thể mở màn hình đăng ký!");
        }
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Thiết lập ban đầu cho các ComboBox và trường mật khẩu
        roleComboBox.getItems().addAll("Bidder", "Seller", "Admin");
        passwordField.setVisible(true);
        passwordTextField.setVisible(false);
        passwordField.managedProperty().bind(passwordField.visibleProperty());
        passwordTextField.managedProperty().bind(passwordTextField.visibleProperty());
    }
}