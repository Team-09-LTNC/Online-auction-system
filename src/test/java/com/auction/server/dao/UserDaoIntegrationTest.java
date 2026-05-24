package com.auction.server.dao;

import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.User;
import com.auction.server.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoIntegrationTest extends DaoIntegrationTestSupport {

    private final UserDao userDao = new UserDao();

    @Test
    void luuNguoiDungAndTimTheoTenDangNhapWork() {
        String username = "it_bidder_" + System.currentTimeMillis();
        try {
            Bidder bidder = new Bidder(username, "secret", "Integration Bidder");

            boolean created = userDao.luuNguoiDung(bidder);
            assertTrue(created);

            Optional<User> found = userDao.timTheoTenDangNhap(username);
            assertTrue(found.isPresent());
            assertEquals("BIDDER", found.get().getRoleName());
            assertEquals("ACTIVE", found.get().getStatus());
        } finally {
            xoaNguoiDungTheoUsername(username);
        }
    }

    @Test
    void capNhatSoDuUpdatesBalance() {
        String username = "it_balance_" + System.currentTimeMillis();
        try {
            Bidder newBidder = new Bidder(username, "secret", "Balance Bidder");
            assertTrue(userDao.luuNguoiDung(newBidder));

            Optional<User> bidderOpt = userDao.timTheoTenDangNhap(username);
            assertTrue(bidderOpt.isPresent());

            User bidder = bidderOpt.get();
            long newBalance = 123_456_789L;
            assertTrue(userDao.capNhatSoDu(bidder.getId(), newBalance));

            Optional<User> reloaded = userDao.timTheoTenDangNhap(username);
            assertTrue(reloaded.isPresent());
            assertEquals(newBalance, reloaded.get().getBalance());
        } finally {
            xoaNguoiDungTheoUsername(username);
        }
    }

    private void xoaNguoiDungTheoUsername(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException ignored) {
        }
    }
}
