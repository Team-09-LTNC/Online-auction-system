package com.auction.ui.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

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
        String selectedRole = roleComboBox.getValue();
        if (usernameField.getText().isBlank()) {
            showError("Lỗi: Hãy nhập tên đăng nhập của bạn!");
        } else if (passwordField.getText().isBlank() && passwordTextField.getText().isBlank()) {
            showError("Lỗi: Vui lòng nhập mật khẩu!");
        } else if (selectedRole == null) {
            showError("Lỗi: Vui lòng chọn vai trò của bạn!");
        } else {
            // TODO: Gọi service/server để xác thực
            statusLabel.setText("Đang kiểm tra đăng nhập cho: " + usernameField.getText());
            statusLabel.setStyle("-fx-text-fill: green;");
        }
    }

    @FXML
    protected void onRegisterLinkClick() {
        try {
            Stage stage = (Stage) registerLink.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Register.fxml"));

            // Thay đổi nội dung của Scene hiện tại thay vì tạo Scene mới
            stage.getScene().setRoot(loader.load());
            stage.setTitle("HỆ THỐNG ĐẤU GIÁ");

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
        roleComboBox.getItems().addAll("Bidder", "Seller", "Admin");
        passwordField.setVisible(true);
        passwordTextField.setVisible(false);
        passwordField.managedProperty().bind(passwordField.visibleProperty());
        passwordTextField.managedProperty().bind(passwordTextField.visibleProperty());
    }
}