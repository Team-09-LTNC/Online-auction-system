package com.auction.client.controller.auth;

import com.auction.client.network.ClientSocket;
import com.auction.common.dto.BaseDTOs;
import com.auction.common.enums.ActionType;
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

        // B4: Thực thi gửi gói tin qua Socket lên Server (Sửa: Dùng Singleton + DTO)
        sendDataToServer(fullName, user, pass, role, event);
    }

    private void sendDataToServer(String fullName, String user, String pass, String role, ActionEvent event) {
        //dùng Singleton để duy trì "đường ống" duy nhất
        try {
            // Controller chỉ lo giao diện, Network lo Socket
            BaseDTOs.Request regReq = new BaseDTOs.Request();
            regReq.type = ActionType.REGISTER; // Đã dùng hằng số chuẩn từ Common
            regReq.requestId = UUID.randomUUID().toString();

            //Server sẽ bóc tách các trường này từ JSON
            // Thực thi gửi qua Singleton Socket
            ClientSocket.getInstance().sendRequest(regReq);

            updateStatus("Đang gửi yêu cầu đăng ký...", "blue");

        } catch (Exception e) {
            // Xử lý ngoại lệ nếu Server không phản hồi
            updateStatus("Lỗi kết nối Network!", "red");
        }
    }

    @FXML
    void onShowPasswordButtonClick(ActionEvent event) {
        // Thực thi logic hoán đổi hiển thị giữa PasswordField và TextField
        togglePassword(passwordField, passwordTextField);
    }

    @FXML
    void onShowConfirmPasswordButtonClick(ActionEvent event) {
        // Thực thi logic hoán đổi hiển thị cho phần xác nhận
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
        // ĐÃ FIX: Lỗi co màn hình (Shrink Issue) theo giải pháp Clean Code (setRoot)
        switchScene(event, "/fxml/auth/Login.fxml", "Đăng nhập tài khoản");
    }

    /**
     * Hàm dùng chung để chuyển đổi màn hình (Scene) - Giải pháp chống Shrink Screen
     */
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            URL fxmlLocation = getClass().getResource(fxmlPath);
            if (fxmlLocation == null) {
                throw new IOException("Không tìm thấy file FXML tại: " + fxmlPath);
            }
            // Tải Root mới
            Parent newRoot = FXMLLoader.load(fxmlLocation);

            // Lấy Scene hiện tại thay vì tạo Scene mới
            Scene currentScene = ((Node) event.getSource()).getScene();

            // Thay đổi Root của Scene hiện tại để giữ nguyên kích thước cửa sổ
            currentScene.setRoot(newRoot);

            // Cập nhật Title cho Stage
            Stage stage = (Stage) currentScene.getWindow();
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            updateStatus("Lỗi tải giao diện!", "red");
        }
    }

    private void updateStatus(String message, String color) {
        // Thực thi cập nhật thuộc tính Style và Text cho Label trạng thái
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + color + ";");
    }
}