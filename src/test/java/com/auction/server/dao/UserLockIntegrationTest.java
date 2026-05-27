package com.auction.server.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.auction.common.exception.AuthenticationException;
import com.auction.common.model.user.User;
import com.auction.server.db.DatabaseConnection;
import com.auction.server.manager.UserManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

class UserLockIntegrationTest extends DaoIntegrationTestSupport {

    private static final DateTimeFormatter LOCK_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Test
    void temporaryPenaltyWritesLockStatusAndUnlockTimeToUsers() throws Exception {
        String username = "lock_bidder_" + System.nanoTime();
        int userId = insertUser(username, "BIDDER", "ACTIVE", null);

        try {
            BidderPenaltyDao.SanctionResult sanction =
                    new BidderPenaltyDao().recordLatePaymentViolation(userId, "test late payment");

            assertThat(sanction.permanentLock).isFalse();
            assertThat(sanction.lockUntil).isNotNull();
            assertThat(userStatus(userId)).isEqualTo("LOCKED");
            assertThat(userLockUntil(userId)).isNotNull();
            assertThat(userLockUntil(userId)).isAfter(LocalDateTime.now());
        } finally {
            deleteUser(userId);
        }
    }

    @Test
    void loginReportsTemporaryLockDurationAndUnlockTime() throws Exception {
        String username = "temp_locked_" + System.nanoTime();
        int userId = insertUser(username, "BIDDER", "ACTIVE", null);

        try {
            BidderPenaltyDao.SanctionResult sanction =
                    new BidderPenaltyDao().recordLatePaymentViolation(userId, "test late payment");

            AuthenticationException ex = assertThrows(
                    AuthenticationException.class,
                    () -> UserManager.getInstance().login(username, "pw", "BIDDER"));

            assertThat(ex.getMessage()).contains("khóa tạm thời");
            assertThat(ex.getMessage()).contains("Thời gian còn lại");
            assertThat(ex.getMessage()).contains("Bạn có thể đăng nhập lại lúc");
            assertThat(ex.getMessage()).contains(sanction.lockUntil.format(LOCK_TIME_FORMAT));
        } finally {
            deleteUser(userId);
        }
    }

    @Test
    void permanentLockReportsContactAdmin() throws Exception {
        String username = "permanent_locked_" + System.nanoTime();
        int userId = insertUser(username, "SELLER", "LOCKED", null);

        try {
            AuthenticationException ex = assertThrows(
                    AuthenticationException.class,
                    () -> UserManager.getInstance().login(username, "pw", "SELLER"));

            assertThat(ex.getMessage())
                    .isEqualTo("Tài khoản đang bị khóa vĩnh viễn, vui lòng liên hệ Admin.");
        } finally {
            deleteUser(userId);
        }
    }

    @Test
    void expiredTemporaryLockIsClearedOnSuccessfulLogin() throws Exception {
        String username = "expired_lock_" + System.nanoTime();
        int userId = insertUser(
                username,
                "BIDDER",
                "LOCKED",
                LocalDateTime.now().minusMinutes(1));

        try {
            User user = UserManager.getInstance().login(username, "pw", "BIDDER");

            assertThat(user.getStatus()).isEqualTo("ACTIVE");
            assertThat(user.getLockUntil()).isNull();
            assertThat(userStatus(userId)).isEqualTo("ACTIVE");
            assertThat(userLockUntil(userId)).isNull();
        } finally {
            UserManager.getInstance().logout(userId);
            deleteUser(userId);
        }
    }

    private int insertUser(String username, String role, String status, LocalDateTime lockUntil)
            throws Exception {
        new UserDao().ensureLockUntilColumn();
        String sql = "INSERT INTO users (username, password, full_name, role, balance, status, lock_until) "
                + "VALUES (?, 'pw', ?, ?, 0, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, "User " + username);
            ps.setString(3, role);
            ps.setString(4, status);
            if (lockUntil != null) {
                ps.setTimestamp(5, Timestamp.valueOf(lockUntil));
            } else {
                ps.setTimestamp(5, null);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                return keys.getInt(1);
            }
        }
    }

    private String userStatus(int userId) throws Exception {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getString("status");
            }
        }
    }

    private LocalDateTime userLockUntil(int userId) throws Exception {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT lock_until FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                Timestamp ts = rs.getTimestamp("lock_until");
                return ts != null ? ts.toLocalDateTime() : null;
            }
        }
    }

    private void deleteUser(int userId) throws Exception {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM bidder_penalties WHERE bidder_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
        }
    }
}
