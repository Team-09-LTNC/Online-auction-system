package com.auction.common.dto;

// Phản hồi từ Server
public class LoginResponse extends Response {
    private String role; // Vai trò của người dùng (ADMIN, SELLER, hoặc BIDDER)

    public LoginResponse(boolean success, String message, String role) {
        super(success, message); // Gọi constructor của lớp cha để gán trạng thái và thông báo
        this.role = role;
    }

    public String getRole() { return role; }
}