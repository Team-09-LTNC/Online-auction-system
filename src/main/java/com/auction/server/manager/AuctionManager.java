package com.auction.server.manager;

import com.auction.common.exception.AuthenticationException;
import com.auction.common.model.user.User;
import com.auction.server.dao.UserDao;
import com.auction.server.utils.DatabaseConnection;

public class AuctionManager {
    private static AuctionManager instance;
    private UserDao userDao;

    private AuctionManager() {
        this.userDao = new UserDao();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) instance = new AuctionManager();
        return instance;
    }

    public User authenticate(String username, String password, String role) throws AuthenticationException {
        User user = userDao.findByUsername(username);

        // 1. Kiểm tra tài khoản không tồn tại [cite: 28-31]
        if (user == null) {
            throw new AuthenticationException("Tài khoản không tồn tại!");
        }

        // 2. Kiểm tra đúng tên/mật khẩu/vai trò [cite: 33-37]
        if (!user.getPassword().equals(password) || !user.getRoleName().equalsIgnoreCase(role)) {
            throw new AuthenticationException("Tên đăng nhập hoặc mật khẩu chưa chính xác!");
        }

        return user;
    }

    public boolean register(User user) {
        if (userDao.findByUsername(user.getUsername()) != null) {
            return false;
        }
        return userDao.saveUser(user);
    }
}