package com.auction.server.dao;

import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.user.Bidder;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionDaoIntegrationTest extends DaoIntegrationTestSupport {

    private final AuctionDao auctionDao = new AuctionDao();

    @Test
    void layDanhSachPhienDangChayReturnsDataFromSeed() {
        List<Auction> auctions = auctionDao.layDanhSachPhienDangChay();
        assertFalse(auctions.isEmpty());
    }

    @Test
    void thucHienGiaoDichDatGiaUpdatesCurrentPrice() {
        Auction auction = auctionDao.layPhienTheoId(1);
        assertNotNull(auction);

        long nextBid = auction.getCurrentHighestBid() + auction.getItem().getBidIncrement();
        Bidder bidder = new Bidder("bidder1", "123456", "Nguoi Mua So 1");
        bidder.setId(3);

        boolean ok = auctionDao.thucHienGiaoDichDatGia(
                auction.getId(),
                new BidTransaction(auction.getId(), bidder, nextBid, LocalDateTime.now())
        );
        assertTrue(ok);

        Auction updated = auctionDao.layPhienTheoId(auction.getId());
        assertNotNull(updated);
        assertEquals(nextBid, updated.getCurrentHighestBid());
    }
}
