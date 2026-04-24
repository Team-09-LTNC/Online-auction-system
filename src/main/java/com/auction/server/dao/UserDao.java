package com.auction.server.dao;

import com.auction.common.model.user.*;
import java.sql.*;

public class UserDao {
    private Connection conn;

    public UserDao(Connection conn) {
        this.conn = conn;
    }

    // LƯU NGƯỜI DÙNG KHI ĐĂNG KÝ
    public boolean saveUser(User user) {
        String sql = "INSERT INTO users (id, username, password, full_name, role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, user.getRoleName().toUpperCase());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi lưu vào Database: " + e.getMessage());
            return false;
        }
    }

    // TÌM NGƯỜI DÙNG ĐỂ ĐĂNG NHẬP
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                String pass = rs.getString("password");
                String name = rs.getString("full_name");
                String id = rs.getString("id");

                User user;
                if ("ADMIN".equals(role)) user = new Admin(username, pass, name);
                else if ("SELLER".equals(role)) user = new Seller(username, pass, name);
                else user = new Bidder(username, pass, name);

                user.setId(id);
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}