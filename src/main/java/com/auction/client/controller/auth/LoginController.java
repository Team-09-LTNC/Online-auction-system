package com.auction.client.controller.auth;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;

public class LoginController {
    // Ánh xạ các ID từ SceneBuilder vào code.
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordTextField; // Ô nhập mật khẩu khi nhấn "Hiện"
    @FXML
    private ComboBox<String> roleComboBox;
    @FXML
    private Label statusLabel;
    @FXML
    private Button showPasswordButton;

    // Thông tin Server
    private final String SERVER_HOST = "localhost";
    private final int SERVER_PORT = 1234;

    @FXML
    public void initialize() {
        // Nạp dữ liệu cho ComboBox vai trò khi giao diện vừa hiện lên
        if (roleComboBox != null) {
            roleComboBox.setItems(FXCollections.observableArrayList("Bidder", "Seller", "Admin"));
        }
    }

    // --- Hàm xử lý Đăng nhập (Khớp với onAction="#onLogInButtonClick" trong FXML) ---
    @FXML
    private void onLogInButtonClick(ActionEvent event) {
        String user = usernameField.getText();

        // Lấy pass từ ô đang hiển thị (ô ẩn hoặc ô hiện)
        String pass = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();

        String role = roleComboBox.getValue();

        // Bước 1: Kiểm tra đầu vào cơ bản
        if (user.isEmpty() || pass.isEmpty() || role == null) {
            statusLabel.setText("Vui lòng nhập đủ thông tin và chọn vai trò!");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;"); // Chữ đỏ báo lỗi
            return;
        }

        // Bước 2: Gửi yêu cầu xác thực lên Server qua Socket
        if (askServerToLogin(user, pass, role)) {
            // Bước 3: Nếu Server ok, chuyển màn hình
            navigateToHome(event);
        } else {
            // Nếu lỗi (Sai tài khoản hoặc Server chưa bật)
            statusLabel.setText("Sai tài khoản/mật khẩu hoặc Server lỗi!");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    /**
     * Hàm này đóng vai trò gửi tin nhắn qua Socket cho Server
     */
    private boolean askServerToLogin(String username, String password, String role) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // Gửi một "gói tin" định dạng: LOGIN|username|password|role
            String request = "LOGIN|" + username + "|" + password + "|" + role;
            out.writeUTF(request);
            out.flush();

            // Đợi Server trả lời (Server phải trả về "SUCCESS" hoặc "FAIL")
            String response = in.readUTF();
            return "SUCCESS".equalsIgnoreCase(response);

        } catch (IOException e) {
            System.out.println("Lỗi kết nối Server: " + e.getMessage());
            return false;
        }
    }

    // --- Hàm hiện/ẩn mật khẩu (Khớp với onAction="#onShowPasswordButtonClick") ---
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

    // --- Quay về trang chủ (Khớp với onAction="#onBackToHomeClick") ---
    @FXML
    private void onBackToHomeClick(ActionEvent event) {
        System.out.println("Quay về trang chủ...");
        // Đã sửa đường dẫn trỏ vào folder fxml trong resources
        switchScene(event, "/fxml/bidder/MainLayout.fxml", "Trang chủ Đấu giá");
    }

    // --- Chuyển sang trang Đăng ký (Khớp với onAction="#onRegisterLinkClick") ---
    @FXML
    private void onRegisterLinkClick(ActionEvent event) {
        System.out.println("Chuyển sang màn hình Đăng ký...");
        // Đã sửa đường dẫn trỏ vào folder fxml trong resources
        switchScene(event, "/fxml/auth/Register.fxml", "Đăng ký tài khoản");
    }

    /**
     * Hàm dùng chung để chuyển đổi màn hình (Scene)
     */
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            URL fxmlLocation = getClass().getResource(fxmlPath);
            if (fxmlLocation == null) {
                throw new IOException("Không tìm thấy file FXML tại: " + fxmlPath);
            }
            Parent root = FXMLLoader.load(fxmlLocation);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể load file: " + fxmlPath + "\nM hãy kiểm tra lại thư mục resources/fxml/!");
        }
    }

    /**
     * Hàm phụ trách việc chuyển sang giao diện chính (Home/Admin) sau khi đăng nhập
     */
    private void navigateToHome(ActionEvent event) {
        // M đang để đường dẫn là /fxml/admin/MainLayout.fxml
        switchScene(event, "/fxml/admin/MainLayout.fxml", "Hệ thống Đấu giá - Admin");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}