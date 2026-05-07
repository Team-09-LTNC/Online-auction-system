package com.auction.common.model.user;

import com.auction.common.model.entity.Entity;

// Lớp cha chứa thông tin chung của mọi loại tài khoản
public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String fullName;
    protected long balance;

    public User(String username, String password, String fullName) {
        super(); // Gọi constructor của Entity để tạo UUID
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.balance = 0; // Mặc định số dư = 0 (phục vụ cho việc trả tiền đấu giá)
    }

    // Bắt buộc class con phải khai báo Role
    public abstract String getRoleName();

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getPassword() { return password; }

    // Thêm getter/setter cho balance (cần thiết cho UserDao)
    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }
}