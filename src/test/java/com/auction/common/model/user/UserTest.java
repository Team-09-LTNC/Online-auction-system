package com.auction.common.model.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void roleNamesAreCorrect() {
        assertEquals("BIDDER", new Bidder("b", "p", "Bidder").getRoleName());
        assertEquals("SELLER", new Seller("s", "p", "Seller").getRoleName());
        assertEquals("ADMIN", new Admin("a", "p", "Admin").getRoleName());
    }

    @Test
    void balanceOperationsFollowRules() {
        Bidder bidder = new Bidder("b", "p", "Bidder");
        assertEquals(0, bidder.getBalance());

        bidder.addBalance(1_000);
        bidder.addBalance(-100);
        assertEquals(1_000, bidder.getBalance());

        assertTrue(bidder.hasEnoughBalance(700));
        assertFalse(bidder.hasEnoughBalance(2_000));

        assertTrue(bidder.deductBalance(300));
        assertEquals(700, bidder.getBalance());

        assertFalse(bidder.deductBalance(1_000));
        assertEquals(700, bidder.getBalance());
    }

    @Test
    void defaultStatusIsActiveAndCanBeChanged() {
        Seller seller = new Seller("s", "p", "Seller");
        assertEquals("ACTIVE", seller.getStatus());

        seller.setStatus("LOCKED");
        assertEquals("LOCKED", seller.getStatus());
    }
}
