package com.auction.client.util;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionTimeUtilTest {

    @Test
    void parsesIsoTimeWithoutSeconds() {
        assertEquals(
                LocalDateTime.of(2026, 5, 23, 10, 15),
                AuctionTimeUtil.parse("2026-05-23T10:15")
        );
    }

    @Test
    void countsDownUntilOpenFromServerClock() {
        AuctionTimeUtil.AuctionState state = AuctionTimeUtil.calculateState(
                "2026-05-23T10:16",
                "2026-05-23T11:00",
                "2026-05-23T10:15"
        );

        assertEquals("OPEN", state.finalStatus);
        assertEquals(60, state.countdownSeconds);
    }

    @Test
    void countsDownUntilEndFromServerClock() {
        AuctionTimeUtil.AuctionState state = AuctionTimeUtil.calculateState(
                "2026-05-23T10:00",
                "2026-05-23T11:00",
                "2026-05-23T10:15"
        );

        assertEquals("RUNNING", state.finalStatus);
        assertEquals(2700, state.countdownSeconds);
    }
}
