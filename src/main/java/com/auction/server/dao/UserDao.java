package com.auction.server.dao;

// Các cột cần trong bảng users:
// id	(INT	PRIMARY KEY, AUTO_INCREMENT	ID) : duy nhất định danh người dùng
//username	(VARCHAR(50)	NOT NULL, UNIQUE) : Tên đăng nhập
//password	(VARCHAR(255)	NOT NULL) : Mật khẩu .
//full_name	(VARCHAR(100)	NOT NULL) : Họ và tên .
//role	(ENUM	'ADMIN', 'SELLER', 'BIDDER') : Vai trò người dùng
//balance	(BIGINT	DEFAULT 0) : Số dư tài khoản

import com.auction.common.model.user.*;
import com.auction.server.db.ConnectionProvider;
import com.auction.server.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp này được thiết kế cho cả 3 vai trò (riêng admin thì k xử lý phần số dư, k hiển thị số dư)
 * Phần BidderSellerMoney mới thực hiện giao dịch tiền bạc giưa bidder vs seller
 */
public class UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);
    /**
     * Lấy thông tin User để phục vụ Đăng nhập.
     */
    public Optional<User> timTheoTenDangNhap(String tenDangNhap) {
        // TỐI ƯU: Thêm cột status vào câu truy vấn duy nhất
        String sql = "SELECT id, username, password, full_name, role, balance, status FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenDangNhap);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String vaiTro = rs.getString("role").toUpperCase();
                    String matKhau = rs.getString("password");
                    String hoTen = rs.getString("full_name");
                    long soDu = rs.getLong("balance");
                    String trangThai = rs.getString("status"); // Đọc trạng thái

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
                    user.setId(id);
                    user.setStatus(trangThai != null ? trangThai : "ACTIVE");
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi timTheoTenDangNhap: ", e);
        }
        return Optional.empty();
    }

    /**
     * Lưu người dùng mới (Đăng ký) với số dư mặc định là 0.
     */
    public boolean luuNguoiDung(User user) {
        String sql = "INSERT INTO users (username, password, full_name, role, balance) VALUES (?, ?, ?, ?, 0)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getRoleName());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi luuNguoiDung: ", e);
            return false;
        }
    }

    /**
     * Cập nhật số dư cho 1 người dùng cụ thể (Dùng khi nạp tiền / rút tiền).
     */
    public boolean capNhatSoDu(int idUser, long soDuMoi) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, soDuMoi);
            pstmt.setInt(2, idUser);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi capNhatSoDu: ", e);
            return false;
        }
    }

    public List<User> layTatCaBidder() {
    List<User> list = new ArrayList<>();
    String sql = "SELECT * FROM users WHERE role = 'BIDDER'";
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            Bidder b = new Bidder(
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("full_name")
            );
            b.setStatus(rs.getString("status") != null ? rs.getString("status") : "ACTIVE"); // ← THÊM
            list.add(b);
        }
    } catch (SQLException e) {
        logger.error("Lỗi lấy danh sách bidder: {}", e.getMessage());
    }
    return list;
}

    public List<User> layTatCaSeller() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'SELLER'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Seller s = new Seller(
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("full_name")
                );
                s.setStatus(rs.getString("status") != null ? rs.getString("status") : "ACTIVE"); // ← THÊM
                list.add(s);
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy danh sách seller: {}", e.getMessage());
        }
        return list;
    }

// Admin có thể khóa tài khoản người dùng (đổi status thành ACTIVE hoặc LOCKED), không xóa hẳn để giữ lịch sử giao dịch.
    public boolean capNhatTrangThai(String username, String status) {
        String sql = "UPDATE users SET status = ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi cập nhật trạng thái user: {}", e.getMessage());
            return false;
        }
    }

    public boolean capNhatTrangThaiTheoId(int userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Loi cap nhat trang thai user theo id: {}", e.getMessage());
            return false;
        }
    }
}
