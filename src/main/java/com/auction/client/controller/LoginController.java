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
                // Gọi AuctionManager để xác thực trực tiếp với Database [cite: 28, 37]
                User user = AuctionManager.getInstance().authenticate(username, password, selectedRole);

                statusLabel.setText("✅ Đăng nhập thành công! Chào " + user.getFullName());
                statusLabel.setStyle("-fx-text-fill: green;");

                try {
                    // 1. Tải file giao diện đấu giá
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/AuctionListScreen.fxml"));
                    javafx.scene.Parent auctionView = loader.load();

                    // 2. Lấy Stage hiện tại
                    javafx.stage.Stage stage = (javafx.stage.Stage) statusLabel.getScene().getWindow();

                    // 3. Tạo Scene mới và đổi màn hình
                    javafx.scene.Scene scene = new javafx.scene.Scene(auctionView);
                    stage.setScene(scene);
                    stage.centerOnScreen();
                    stage.show();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println("Lỗi khi chuyển sang màn hình Đấu giá: " + ex.getMessage());
                }
            } catch (AuthenticationException e) {
                // Hiển thị lỗi theo yêu cầu của bạn [cite: 31, 37]
                showError(e.getMessage());
            }
        }
    }

    @FXML
    protected void onRegisterLinkClick() {
        try {
            Stage stage = (Stage) registerLink.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Register.fxml"));
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