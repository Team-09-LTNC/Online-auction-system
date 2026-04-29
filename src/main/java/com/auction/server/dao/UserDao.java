package com.auction.server.dao;

import com.auction.common.model.user.*;
import com.auction.server.utils.DatabaseConnection;

import java.sql.*;

public class UserDao {

    // Lưu người dùng khi đăng ký
    public boolean saveUser(User user) {
        String sql = "INSERT INTO users (id, username, password, full_name, role) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

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

    // Tìm người dùng để đăng nhập
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    String pass = rs.getString("password");
                    String name = rs.getString("full_name");
                    String id   = rs.getString("id");

                    User user;
                    if ("ADMIN".equals(role))       user = new Admin(username, pass, name);
                    else if ("SELLER".equals(role)) user = new Seller(username, pass, name);
                    else                            user = new Bidder(username, pass, name);

                    user.setId(id);
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi tìm user: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
