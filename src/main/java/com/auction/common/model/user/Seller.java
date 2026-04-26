package com.auction.common.model.user;

// Tài khoản của người bán
public class Seller extends User {

    public Seller(String username, String password, String fullName) {
        super(username, password, fullName);
    }

    @Override
    public String getRoleName() {
        return "SELLER";
    }
}