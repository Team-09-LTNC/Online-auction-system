package com.auction.common.model.bid;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.item.Electronics;
import com.auction.common.model.user.Bidder;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    @Test
    void updateWinnerUpdatesPriceWinnerAndHistory() {
        Electronics item = new Electronics("Laptop", "Gaming", 1_000_000L, "ASUS", 12);
        Auction auction = new Auction(item);
        Bidder bidder = new Bidder("alice", "pw", "Alice");
        BidTransaction tx = new BidTransaction(1, bidder, 1_200_000L, LocalDateTime.now());

        auction.updateWinner(tx);

        assertEquals(1_200_000L, auction.getCurrentHighestBid());
        assertEquals(bidder, auction.getCurrentWinner());
        assertEquals(1, auction.getBidHistory().size());
        assertSame(tx, auction.getBidHistory().getFirst());
    }

    @Test
    void getStatusReturnsOpenWhenBeforeStartTime() {
        Auction auction = new Auction(new Electronics("Phone", "Flagship", 10_000L, "Apple", 12));
        auction.setStartTime(LocalDateTime.now().plusMinutes(5));
        auction.setEndTime(LocalDateTime.now().plusMinutes(15));

        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    @Test
    void getStatusReturnsFinishedWhenAfterEndTime() {
        Auction auction = new Auction(new Electronics("Phone", "Flagship", 10_000L, "Apple", 12));
        auction.setStartTime(LocalDateTime.now().minusMinutes(15));
        auction.setEndTime(LocalDateTime.now().minusMinutes(1));

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    @Test
    void getStatusReturnsRunningWithinTimeWindow() {
        Auction auction = new Auction(new Electronics("Phone", "Flagship", 10_000L, "Apple", 12));
        auction.setStartTime(LocalDateTime.now().minusMinutes(1));
        auction.setEndTime(LocalDateTime.now().plusMinutes(10));

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    void getStatusKeepsTerminalState() {
        Auction auction = new Auction(new Electronics("Phone", "Flagship", 10_000L, "Apple", 12));
        auction.setStatus(AuctionStatus.CANCELED);
        auction.setStartTime(LocalDateTime.now().minusMinutes(1));
        auction.setEndTime(LocalDateTime.now().plusMinutes(10));

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    void isAcceptingBidsOnlyWhenRunning() {
        Auction auction = new Auction(new Electronics("Tablet", "Good", 10_000L, "Samsung", 12));
        auction.setStartTime(LocalDateTime.now().minusMinutes(1));
        auction.setEndTime(LocalDateTime.now().plusMinutes(10));
        assertTrue(auction.isAcceptingBids());

        auction.setStartTime(LocalDateTime.now().plusMinutes(1));
        assertFalse(auction.isAcceptingBids());
    }

    @Test
    void extendEndTimeCreatesOrExtends() {
        Auction auction = new Auction(new Electronics("Camera", "Nice", 10_000L, "Sony", 12));

        auction.extendEndTime(30);
        LocalDateTime firstEnd = auction.getEndTime();
        assertNotNull(firstEnd);

        auction.extendEndTime(10);
        assertEquals(firstEnd.plusSeconds(10), auction.getEndTime());
    }
}
