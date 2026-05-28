package com.auction.server.dao;

// Các cột cần trong bảng users:
// id	(INT	PRIMARY KEY, AUTO_INCREMENT	ID) : duy nhất định danh người dùng
//username	(VARCHAR(50)	NOT NULL, UNIQUE) : Tên đăng nhập
//password	(VARCHAR(255)	NOT NULL) : Mật khẩu .
//full_name	(VARCHAR(100)	NOT NULL) : Họ và tên .
//role	(ENUM	'ADMIN', 'SELLER', 'BIDDER') : Vai trò người dùng
//balance	(BIGINT	DEFAULT 0) : Số dư tài khoản

import com.auction.common.model.user.*;
import com.auction.server.db.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp này được thiết kế cho cả 3 vai trò (riêng admin thì không xử lý phần số dư, không hiển thị số dư)
 * Phần BidderSellerMoney mới thực hiện giao dịch tiền bạc giữa người đặt giá và người bán
 */
public class UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);
    /**
     * Lấy thông tin người dùng để phục vụ đăng nhập.
     */
    public Optional<User> findByUsername(String tenDangNhap) {
        ensureLockUntilColumn();
        // Thêm cột trạng thái vào câu truy vấn duy nhất
        String sql = "SELECT id, username, password, full_name, role, balance, status, lock_until "
                + "FROM users WHERE username = ?";
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
                    Timestamp lockUntil = rs.getTimestamp("lock_until");

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
                    user.setLockUntil(lockUntil != null ? lockUntil.toLocalDateTime() : null);
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
    public boolean saveUser(User user) {
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
    public boolean updateBalance(int idUser, long soDuMoi) {
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

    public List<User> getAllBidders() {
    ensureLockUntilColumn();
    List<User> list = new ArrayList<>();
    String sql = "SELECT id, username, password, full_name, balance, status, lock_until "
            + "FROM users WHERE role = 'BIDDER'";
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            Bidder b = new Bidder(
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("full_name")
            );
            b.setId(rs.getInt("id"));
            b.setBalance(rs.getLong("balance"));
            b.setStatus(rs.getString("status") != null ? rs.getString("status") : "ACTIVE"); // ← THÊM
            Timestamp lockUntil = rs.getTimestamp("lock_until");
            b.setLockUntil(lockUntil != null ? lockUntil.toLocalDateTime() : null);
            list.add(b);
        }
    } catch (SQLException e) {
        logger.error("Lỗi lấy danh sách bidder: {}", e.getMessage());
    }
    return list;
}

    public List<User> getAllSellers() {
        ensureLockUntilColumn();
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, password, full_name, balance, status, lock_until "
                + "FROM users WHERE role = 'SELLER'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Seller s = new Seller(
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("full_name")
                );
                s.setId(rs.getInt("id"));
                s.setBalance(rs.getLong("balance"));
                s.setStatus(rs.getString("status") != null ? rs.getString("status") : "ACTIVE"); // ← THÊM
                Timestamp lockUntil = rs.getTimestamp("lock_until");
                s.setLockUntil(lockUntil != null ? lockUntil.toLocalDateTime() : null);
                list.add(s);
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy danh sách seller: {}", e.getMessage());
        }
        return list;
    }

// Admin có thể khóa tài khoản người dùng (đổi trạng thái thành ACTIVE hoặc LOCKED), không xóa hẳn để giữ lịch sử giao dịch.
    public boolean updateStatus(String username, String status) {
        ensureLockUntilColumn();
        String sql = "UPDATE users SET status = ?, lock_until = NULL WHERE username = ?";
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

    public boolean updateStatusById(int userId, String status) {
        ensureLockUntilColumn();
        String sql = "UPDATE users SET status = ?, lock_until = NULL WHERE id = ?";
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

    public boolean updateTemporaryLockById(int userId, LocalDateTime lockUntil) {
        ensureLockUntilColumn();
        String sql = "UPDATE users SET status = 'LOCKED', lock_until = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(lockUntil));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Loi cap nhat thoi gian khoa tam thoi user theo id: {}", e.getMessage());
            return false;
        }
    }

    public void ensureLockUntilColumn() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            if (hasColumn(conn, "users", "lock_until")) {
                return;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE users ADD COLUMN lock_until DATETIME NULL");
            }
        } catch (SQLException e) {
            logger.error("Khong the dam bao cot lock_until trong bang users: {}", e.getMessage());
        }
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tableName, columnName)) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null,
                tableName.toUpperCase(), columnName.toUpperCase())) {
            return rs.next();
        }
    }
}
