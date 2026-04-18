package com.auction.ui.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController implements Initializable {

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
    protected void onShowPasswordButtonClick() {
        togglePasswordVisibility(passwordField, passwordTextField);
    }

    @FXML
    protected void onShowConfirmPasswordButtonClick() {
        togglePasswordVisibility(confirmPasswordField, confirmPasswordTextField);
    }

    private void togglePasswordVisibility(PasswordField pf, TextField tf) {
        if (!tf.isVisible()) {
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
    protected void onRegisterButtonClick() {
        String fullName       = fullNameField.getText().trim();
        String username       = usernameField.getText().trim();
        String password       = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();
        String confirmPassword = confirmPasswordField.isVisible() ? confirmPasswordField.getText() : confirmPasswordTextField.getText();
        String role           = roleComboBox.getValue();

        if (fullName.isBlank()) { showError("Lỗi: Vui lòng nhập họ và tên!"); return; }
        if (username.isBlank()) { showError("Lỗi: Vui lòng nhập tên đăng nhập!"); return; }
        if (username.length() < 4) { showError("Lỗi: Tên đăng nhập phải có ít nhất 4 ký tự!"); return; }
        if (password.isBlank()) { showError("Lỗi: Vui lòng nhập mật khẩu!"); return; }
        if (password.length() < 6) { showError("Lỗi: Mật khẩu phải có ít nhất 6 ký tự!"); return; }
        if (!password.equals(confirmPassword)) { showError("Lỗi: Mật khẩu xác nhận không khớp!"); return; }
        if (role == null) { showError("Lỗi: Vui lòng chọn vai trò!"); return; }

        // TODO: Gọi service/server để tạo tài khoản
        statusLabel.setText("✅ Đăng ký thành công! Chào mừng " + fullName + " (" + role + ")");
        statusLabel.setStyle("-fx-text-fill: green;");
    }

    @FXML
    protected void onLoginLinkClick() {
        try {
            Stage stage = (Stage) loginLink.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));

            // Thay đổi nội dung của Scene hiện tại thay vì tạo Scene mới
            stage.getScene().setRoot(loader.load());
            stage.setTitle("HỆ THỐNG ĐẤU GIÁ");

        } catch (IOException e) {
            e.printStackTrace();
            showError("Không thể mở màn hình đăng nhập!");
        }
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roleComboBox.getItems().addAll("Bidder", "Seller");

        passwordField.setVisible(true);
        passwordTextField.setVisible(false);
        confirmPasswordField.setVisible(true);
        confirmPasswordTextField.setVisible(false);

        passwordField.managedProperty().bind(passwordField.visibleProperty());
        passwordTextField.managedProperty().bind(passwordTextField.visibleProperty());
        confirmPasswordField.managedProperty().bind(confirmPasswordField.visibleProperty());
        confirmPasswordTextField.managedProperty().bind(confirmPasswordTextField.visibleProperty());
    }
}