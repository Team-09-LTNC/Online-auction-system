package com.auction.manager;

import com.auction.model.bid.Auction;
import com.auction.model.user.User;
import java.util.ArrayList;
import java.util.List;
import com.auction.exception.AuthenticationException;

public class AuctionManager {
    private static AuctionManager instance;
    private List<Auction> auctions;
    private List<User> users;

    private AuctionManager() {
        auctions = new ArrayList<>();
        users = new ArrayList<>();
    }

    // Singleton: Đảm bảo chỉ có một AuctionManager duy nhất trong hệ thống
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void addAuction(Auction auction) {
        auctions.add(auction);
    }

    public List<Auction> getAuctions() {
        return auctions;
    }

    public void addUser(User user) {
        users.add(user);
    }

    public User findUserByUsername(String username) {
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
    }
    public User authenticate(String username, String password, String role) throws AuthenticationException {
        User user = findUserByUsername(username);
        if (user == null) {
            throw new AuthenticationException("Tên đăng nhập không tồn tại!");
        }
        if (!user.getPassword().equals(password)) {
            throw new AuthenticationException("Mật khẩu không chính xác!");
        }
        if (!user.getRoleName().equalsIgnoreCase(role)) {
            throw new AuthenticationException("Tài khoản này không có quyền truy cập với vai trò: " + role);
        }
        return user;
    }
}