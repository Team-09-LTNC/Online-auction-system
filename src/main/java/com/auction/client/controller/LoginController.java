package com.auction.client.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.auction.common.exception.AuthenticationException;
import com.auction.common.model.user.User;
import com.auction.server.manager.AuctionManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
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
    @FXML private Button loginButton;

    // ===== THÊM FIELD CHO NÚT QUAY VỀ =====
    @FXML private Button btnBack;

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

        if (username.isBlank()) { showError("Lỗi: Hãy nhập tên đăng nhập của bạn!"); return; }
        if (password.isBlank()) { showError("Lỗi: Vui lòng nhập mật khẩu!"); return; }
        if (selectedRole == null) { showError("Lỗi: Vui lòng chọn vai trò của bạn!"); return; }

        showInfo("⏳ Đang đăng nhập...");
        setFormDisabled(true);

        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                return AuctionManager.getInstance().authenticate(username, password, selectedRole);
            }
        };

        loginTask.setOnSucceeded(event -> {
            User user = loginTask.getValue();

            // Lưu user vào session
            HomeController.Session.login(user);

            showSuccess("✅ Đăng nhập thành công! Chào " + user.getFullName());

            // Admin: thông báo chưa có giao diện
            if ("Admin".equals(selectedRole)) {
                setFormDisabled(false);
                showError("Chức năng cho Admin đang được phát triển!");
                HomeController.Session.logout();
                return;
            }

            // Tất cả vai trò khác (Bidder, Seller) đều quay về Home
            Task<Void> delay = new Task<>() {
                @Override protected Void call() throws Exception {
                    Thread.sleep(600);
                    return null;
                }
            };
            delay.setOnSucceeded(e -> navigateToHome());
            new Thread(delay).start();
        });

        loginTask.setOnFailed(event -> {
            Throwable ex = loginTask.getException();
            setFormDisabled(false);
            if (ex instanceof AuthenticationException) {
                showError(ex.getMessage());
            } else {
                showError("Lỗi hệ thống: Không thể kết nối. Vui lòng thử lại!");
            }
        });

        new Thread(loginTask).start();
    }

    // ===== XỬ LÝ NÚT QUAY VỀ HOME  =====
    @FXML
    private void onBackToHomeClick() {
        try {
            Stage stage = (Stage) btnBack.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"));
            Parent root = loader.load();

            // Cập nhật giao diện Home
            HomeController homeCtrl = loader.getController();
            homeCtrl.refreshAuthBar();

            stage.getScene().setRoot(root);
            stage.setTitle("HỆ THỐNG ĐẤU GIÁ");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Lỗi: Không thể quay về trang chủ!");
        }
    }

    private void navigateToHome() {
        try {
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"));
            Parent root = loader.load();

            // Gọi refreshAuthBar() để cập nhật lời chào
            HomeController homeCtrl = loader.getController();
            homeCtrl.refreshAuthBar();

            stage.getScene().setRoot(root);
            stage.setTitle("HỆ THỐNG ĐẤU GIÁ");
        } catch (IOException ex) {
            ex.printStackTrace();
            setFormDisabled(false);
            showError("Lỗi hệ thống: Không thể mở giao diện Home!");
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

    private void setFormDisabled(boolean disabled) {
        usernameField.setDisable(disabled);
        passwordField.setDisable(disabled);
        passwordTextField.setDisable(disabled);
        roleComboBox.setDisable(disabled);
        if (loginButton != null) loginButton.setDisable(disabled);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: green;");
    }

    private void showInfo(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #6E1C1C; -fx-font-style: italic;");
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