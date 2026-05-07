package com.auction.common.dto;

public class LoginRequest extends Request {
    private String username;
    private String password;

    public LoginRequest(String username, String password) {
        super("LOGIN"); // Gán nhãn để Server nhận diện luồng xử lý đăng nhập
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}