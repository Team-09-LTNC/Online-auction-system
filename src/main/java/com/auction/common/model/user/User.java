package com.auction.common.model.user;

import com.auction.common.model.entity.Entity;

public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String fullName;
    protected long balance;

    public User(String username, String password, String fullName) {
        super();
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.balance = 0;
    }

    public abstract String getRoleName();

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
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