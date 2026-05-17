package com.auction.common.dto;

import com.auction.common.enums.ActionType;
import com.auction.common.enums.StatusCode;

public class AuthDTOs {

    // --- ĐĂNG NHẬP ---
    public static class LoginRequest extends BaseDTOs.Request {
        private final String username;
        private final String password;

        public LoginRequest(String username, String password) {
            // (AUTH_LOGIN)
            super(ActionType.LOGIN);
            this.username = username;
            this.password = password;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }

    public static class UserDTO {
        private final int id;
        private final String username;
        private final String role;
        private final String fullName;

        public UserDTO(int id, String username, String role, String fullName) {
            this.id = id; this.username = username; this.role = role; this.fullName = fullName;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public String getFullName() { return fullName; }
    }

    public static class LoginResponse extends BaseDTOs.Response {
        private final UserDTO userData;

        public LoginResponse(boolean success, String message, UserDTO userData) {
            super("LOGIN_RESPONSE", StatusCode.OK, success, message);
            this.userData = userData;
        }

        public UserDTO getUserData() { return userData; }
    }

    // --- ĐĂNG KÝ ---
    public static class RegisterRequest extends BaseDTOs.Request {
        private final String username;
        private final String password;
        private final String fullName;
        private final String role;

        public RegisterRequest(String username, String password, String fullName, String role) {
            //  (AUTH_REGISTER)
            super(ActionType.REGISTER);
            this.username = username;
            this.password = password;
            this.fullName = fullName;
            this.role = role;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getFullName() { return fullName; }
        public String getRole() { return role; }
    }

    public static class RegisterResponse extends BaseDTOs.Response {
        public RegisterResponse(boolean success, String message) {
            super("REGISTER_RESPONSE", StatusCode.CREATED, success, message);
        }
    }

    // --- ĐĂNG XUẤT ---
    public static class LogoutRequest extends BaseDTOs.Request {
        private final int userId;

        public LogoutRequest(int userId) {
            super(ActionType.LOGOUT);
            this.userId = userId;
        }

        public int getUserId() { return userId; }
    }
}