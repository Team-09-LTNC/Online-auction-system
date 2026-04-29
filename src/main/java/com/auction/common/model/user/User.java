package com.auction.common.model.user;

import com.auction.common.model.entity.Entity;

// Lớp cha chứa thông tin chung của mọi loại tài khoản
public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String fullName;

    public User(String username, String password, String fullName) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }

    // Bắt buộc class con phải khai báo vai trò (Role)
    public abstract String getRoleName();

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getPassword() { return password; }
}