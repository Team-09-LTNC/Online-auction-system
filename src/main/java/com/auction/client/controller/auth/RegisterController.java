package com.auction.client.controller.auth;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class RegisterController {

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
    public void initialize() {
        // Khởi tạo danh sách vai trò khi màn hình vừa hiện lên
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("Bidder", "Seller", "Admin");
        }
    }

    @FXML
    void onRegisterButtonClick(ActionEvent event) {
        // B1: Trích xuất dữ liệu từ các input field
        String fullName = fullNameField.getText().trim();
        String user = usernameField.getText().trim();

        // B2: Xác định lấy pass từ ô ẩn hay ô hiện (phụ thuộc vào trạng thái nút 👁)
        String pass = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();
        String confirm = confirmPasswordField.isVisible() ? confirmPasswordField.getText() : confirmPasswordTextField.getText();
        String role = roleComboBox.getValue();

        // B3: Logic kiểm tra tính hợp lệ dữ liệu tại phía Client
        if (fullName.isEmpty() || user.isEmpty() || pass.isEmpty() || role == null) {
            updateStatus("Thiếu thông tin!", "red");
            return;
        }

        if (!pass.equals(confirm)) {
            updateStatus("Mật khẩu xác nhận không khớp!", "red");
            return;
        }

        // B4: Thực thi gửi gói tin qua Socket lên Server
        sendDataToServer(fullName, user, pass, role, event);
    }

    private void sendDataToServer(String fullName, String user, String pass, String role, ActionEvent event) {
        // Thiết lập luồng kết nối TCP/IP tới Server
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner in = new Scanner(socket.getInputStream())) {

            // Đóng gói dữ liệu theo format thống nhất để Server split("\\|")
            out.println("REGISTER|" + fullName + "|" + user + "|" + pass + "|" + role);

            // Đợi phản hồi từ luồng Input của Socket
            if (in.hasNextLine()) {
                String response = in.nextLine();
                // Phân tích phản hồi từ Server để điều hướng giao diện
                if (response.equals("REG_SUCCESS")) {
                    updateStatus("Đăng ký thành công!", "green");
                    onLoginLinkClick(event); // Thực thi chuyển cảnh sau khi có tín hiệu thành công
                } else {
                    updateStatus("Lỗi: " + response, "red");
                }
            }
        } catch (Exception e) {
            // Xử lý ngoại lệ nếu Server không phản hồi (chưa bật hoặc lỗi mạng)
            updateStatus("Lỗi: Không kết nối được Server!", "red");
        }
    }

    @FXML
    void onShowPasswordButtonClick(ActionEvent event) {
        // Thực thi logic hoán đổi hiển thị giữa PasswordField và TextField
        togglePassword(passwordField, passwordTextField);
    }

    @FXML
    void onShowConfirmPasswordButtonClick(ActionEvent event) {
        togglePassword(confirmPasswordField, confirmPasswordTextField);
    }

    private void togglePassword(PasswordField pf, TextField tf) {
        // Logic: Chuyển text từ ô ẩn sang ô hiện và ngược lại, sau đó đảo trạng thái Visible
        if (pf.isVisible()) {
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
    void onLoginLinkClick(ActionEvent event) {
        // Luồng chuyển đổi Scene: Tải file FXML -> Lấy Stage hiện tại -> Thay thế Scene
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/auth/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            updateStatus("Lỗi tải giao diện Login!", "red");
        }
    }

    private void updateStatus(String message, String color) {
        // Thực thi cập nhật thuộc tính Style và Text cho Label trạng thái
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + color + ";");
    }
}