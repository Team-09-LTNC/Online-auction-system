package com.auction.server.dao;

import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoIntegrationTest extends DaoIntegrationTestSupport {

    private final UserDao userDao = new UserDao();

    @Test
    void luuNguoiDungAndTimTheoTenDangNhapWork() {
        String username = "it_bidder_" + System.currentTimeMillis();
        Bidder bidder = new Bidder(username, "secret", "Integration Bidder");

        boolean created = userDao.luuNguoiDung(bidder);
        assertTrue(created);

        Optional<User> found = userDao.timTheoTenDangNhap(username);
        assertTrue(found.isPresent());
        assertEquals("BIDDER", found.get().getRoleName());
        assertEquals("ACTIVE", found.get().getStatus());
    }

    @Test
    void capNhatSoDuUpdatesBalance() {
        Optional<User> bidderOpt = userDao.timTheoTenDangNhap("bidder1");
        assertTrue(bidderOpt.isPresent());

        User bidder = bidderOpt.get();
        long newBalance = 123_456_789L;
        assertTrue(userDao.capNhatSoDu(bidder.getId(), newBalance));

        Optional<User> reloaded = userDao.timTheoTenDangNhap("bidder1");
        assertTrue(reloaded.isPresent());
        assertEquals(newBalance, reloaded.get().getBalance());
    }
}
