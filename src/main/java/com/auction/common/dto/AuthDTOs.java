package com.auction.common.dto;

public class AuthDTOs {
    // Đăng nhập
    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class LoginResponse {
        public boolean success;
        public String message;
        public Object userData; // Chứa thông tin User sau khi login
    }

    // Đăng ký
    public static class RegisterRequest {
        public String username;
        public String password;
        public String fullName;
        public String role;
    }

    public static class RegisterResponse {
        public boolean success;
        public String message;
    }

    // Đăng xuất
    public static class LogoutRequest {
        public int userId;
    }
}