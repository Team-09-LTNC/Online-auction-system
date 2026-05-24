package com.auction.client.controller.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserSessionTest {

    @AfterEach
    void resetSession() {
        UserSession.clear();
    }

    @Test
    void setAndGetSessionValues() {
        UserSession.setUserId(123);
        UserSession.setUsername("alice");
        UserSession.setCurrentRole("BIDDER");

        assertEquals(123, UserSession.getUserId());
        assertEquals("alice", UserSession.getUsername());
        assertEquals("BIDDER", UserSession.getCurrentRole());
    }

    @Test
    void clearResetsAllValues() {
        UserSession.setUserId(10);
        UserSession.setUsername("u");
        UserSession.setCurrentRole("ADMIN");

        UserSession.clear();

        assertEquals(0, UserSession.getUserId());
        assertNull(UserSession.getUsername());
        assertNull(UserSession.getCurrentRole());
    }
}
