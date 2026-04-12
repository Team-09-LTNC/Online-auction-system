package com.auction.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;
public class LoginController implements Initializable {
    @FXML
    private Label welcomeText;
// Khai báo biến
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<String> roleComboBox;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField passwordTextField; // Ô hiện mật khẩu
    @FXML
    private Button showPasswordButton; // Nút bấm

    @FXML
    protected void onShowPasswordButtonClick() {
        // Nếu ô hiện đang ẩn (nghĩa là đang ở chế độ giấu mật khẩu)
        if (!passwordTextField.isVisible()) {
            // 1. Lấy text từ ô ẩn bỏ vào ô hiện
            passwordTextField.setText(passwordField.getText());
            // 2. Hiện ô hiện lên, ẩn ô ẩn đi
            passwordTextField.setVisible(true);
            passwordField.setVisible(false);
        } else {
            // Làm ngược lại khi muốn giấu mật khẩu
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordTextField.setVisible(false);
        }
    }
    @FXML
    protected void onLogInButtonClick() {
        String selectedRole = roleComboBox.getValue();
       // Kiểm tra tình trạng ô tên đăng nhập
       if(usernameField.getText().isBlank()){
           statusLabel.setText("Lỗi: Hãy nhập tên dăng nhập của bạn! ");
           statusLabel.setStyle("-fx-text-fill: red;");// Dổi chữ sang màu đỏ
       }
       //Nếu tên đã có, kiểm tra tiếp mật khẩu
       else if (passwordField.getText().isBlank()) {
           statusLabel.setText("Lỗi: Vui lòng tạo mật khẩu! ");
           statusLabel.setStyle("-fx-text-fill: red;");
       } else if (selectedRole == null) {
           statusLabel.setText("Lỗi: Vui lòng chọn vai trò của bạn!");
           statusLabel.setStyle("-fx-text-fill: red;");

       } else {
        //Nếu đã nhập tên ,mật khẩu, vai trò hiện thông báo đang xử lý
        statusLabel.setText("Đang kiểm tra đăng nhập cho: " + usernameField.getText());
        statusLabel.setStyle("-fx-text-fill: green;");
       }
    }


    @Override
    public void initialize(URL url, ResourceBundle rb){
        // Thêm các lựa chọn vào ComboBox
        roleComboBox.getItems().addAll("Bidder", "Seller", "Admin");
        // 2. Thiết lập mặc định cho 2 ô mật khẩu
        passwordField.setVisible(true);        // Ô ẩn hiện lên
        passwordTextField.setVisible(false);   // Ô hiện giấu đi

        // 3. Đảm bảo chúng chiếm chỗ của nhau
        passwordField.managedProperty().bind(passwordField.visibleProperty());
        passwordTextField.managedProperty().bind(passwordTextField.visibleProperty());


    }
}
