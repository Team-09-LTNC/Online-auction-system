package com.auction.common.model.user;

// Tài khoản quản trị viên để quản lý, có quyền thêm hoặc xóa caác phiên đấu giá
public class Admin extends User {

    public Admin(String username, String password, String fullName) {
        super(username, password, fullName);
    }

    @Override
    public String getRoleName() {
        return "ADMIN";
    }
}