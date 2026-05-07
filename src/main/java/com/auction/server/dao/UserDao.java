package com.auction.server.dao;

// Các cột cần trong bảng users:
// id	(INT	PRIMARY KEY, AUTO_INCREMENT	ID) : duy nhất định danh người dùng
//username	(VARCHAR(50)	NOT NULL, UNIQUE) : Tên đăng nhập
//password	(VARCHAR(255)	NOT NULL) : Mật khẩu .
//full_name	(VARCHAR(100)	NOT NULL) : Họ và tên .
//role	(ENUM	'ADMIN', 'SELLER', 'BIDDER') : Vai trò người dùng
//balance	(BIGINT	DEFAULT 0) : Số dư tài khoản

import com.auction.common.model.user.*;
import com.auction.server.utils.DatabaseConnection;
import java.sql.*;
import java.util.Optional;

/**
 * Lớp này được thiết kế cho cả 3 vai trò (riêng admin thì k xử lý phần số dư, k hiển thị số dư)
 * Phần BidderSellerMoney mới thực hiện giao dịch tiền bạc giưa bidder vs seller
 */
public class UserDao {

    /**
     * Lấy thông tin User để phục vụ Đăng nhập.
     */
    public Optional<User> timTheoTenDangNhap(String tenDangNhap) {
        String sql = "SELECT id, username, password, full_name, role, balance FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenDangNhap);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String vaiTro = rs.getString("role").toUpperCase();
                    String matKhau = rs.getString("password");
                    String hoTen = rs.getString("full_name");
                    long soDu = rs.getLong("balance");

                    User user;
                    switch (vaiTro) {
                        case "ADMIN":
                            user = new Admin(tenDangNhap, matKhau, hoTen);
                            break;
                        case "SELLER":
                            user = new Seller(tenDangNhap, matKhau, hoTen);
                            user.setBalance(soDu);
                            break;
                        case "BIDDER":
                            user = new Bidder(tenDangNhap, matKhau, hoTen);
                            user.setBalance(soDu);
                            break;
                        default:
                            throw new IllegalStateException("Unknown role: " + vaiTro);
                    }
                    user.setId(rs.getInt("id"));
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi timTheoTenDangNhap: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Lưu người dùng mới (Đăng ký) với số dư mặc định là 0.
     */
    public boolean luuNguoiDung(User user) {
        String sql = "INSERT INTO users (username, password, full_name, role, balance) VALUES (?, ?, ?, ?, 0)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getRoleName());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi luuNguoiDung: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật số dư cho 1 người dùng cụ thể (Dùng khi nạp tiền / rút tiền).
     */
    public boolean capNhatSoDu(int idUser, long soDuMoi) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, soDuMoi);
            pstmt.setInt(2, idUser);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi capNhatSoDu: " + e.getMessage());
            return false;
        }
    }
}