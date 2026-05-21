package com.auction.common.model.user;

import com.auction.common.model.entity.Entity;

public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String fullName;
    protected long balance;
    protected String status; // ACTIVE, LOCKED

    public User(String username, String password, String fullName) {
        super();
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.balance = 0;
        this.status = "ACTIVE"; // Mặc định là hoạt động
    }

    // public User(String username, String fullName) {
    //     super();
    //     this.username = username;
    //     this.fullName = fullName;
    //     this.status = "ACTIVE"; // Mặc định là hoạt động
    // }

    public abstract String getRoleName();

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPassword() { return password; }

    // Đảm bảo Thread-safety cho số dư
    public synchronized long getBalance() { return balance; }
    public synchronized void setBalance(long balance) { this.balance = balance; }

    public synchronized boolean hasEnoughBalance(long amount) {
        return this.balance >= amount;
    }

    // Đồng bộ hóa để tránh Lost Update
    public synchronized boolean deductBalance(long amount) {
        if (hasEnoughBalance(amount)) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    public synchronized void addBalance(long amount) {
        if (amount > 0) {
            this.balance += amount;
        }
    }
}