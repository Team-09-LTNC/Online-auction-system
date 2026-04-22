package com.auction.model.user;

// Tài khoản của người mua (được phép đặt giá)
public class Bidder extends User {
    public Bidder(String username, String password, String fullName) {
        super(username, password, fullName);
    }

    @Override
    public String getRoleName() {
        return "BIDDER";
    }

}