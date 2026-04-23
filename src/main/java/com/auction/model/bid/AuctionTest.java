package com.auction.model.bid;

import java.time.LocalDateTime;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.auction.auction.AuctionStatus;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.item.Art;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class AuctionTest {

    private Auction auction;
    private Bidder bidder1;
    private Bidder bidder2;

    // trạng thái ban đầu
    @BeforeEach
    public void setUp() {
        Seller seller = new Seller("seller1", "123", "Người Bán");
        Item item = new Art("Tranh", "Mô tả", 100.0, "TG", 2023, "Sơn");

        bidder1 = new Bidder("b1", "123", "Người Mua 1");
        bidder2 = new Bidder("b2", "123", "Người Mua 2");

        auction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        // mặc định m phiên đấu giá để test
        auction.updateStatus(AuctionStatus.RUNNING);
    }

    // test đặt giá hợp lệ
    @Test
    public void testAddValidBid_Success() {
        BidTransaction bid = new BidTransaction(bidder1, 150.0);
        assertDoesNotThrow(() -> {
            boolean result = auction.addValidBid(bid);

            assertTrue(result);
            assertEquals(150.0, auction.getCurrentHighestBid());
            assertEquals(bidder1, auction.getCurrentWinner());
        });
    }

    // test đặt giá thấp hơn
    @Test
    public void testAddValidBid_LowerPrice_ThrowsException() {
        assertDoesNotThrow(() -> auction.addValidBid(new BidTransaction(bidder1, 150.0)));

        BidTransaction invalidBid = new BidTransaction(bidder2, 120.0);

        assertThrows(InvalidBidException.class, () -> {
            auction.addValidBid(invalidBid);
        });
    }

    // test phiên đóng ném exception
    @Test
    public void testAddValidBid_AuctionClosed_ThrowsException() {
        auction.updateStatus(AuctionStatus.FINISHED); // Ép đóng phiên
        BidTransaction bid = new BidTransaction(bidder1, 500.0);

        assertThrows(InvalidBidException.class, () -> {
            auction.addValidBid(bid);
        });
    }

    // test đóng phiên hợp lệ
    @Test
    public void testCloseAuction_Success() {
        assertDoesNotThrow(() -> {
            auction.addValidBid(new BidTransaction(bidder1, 150.0));
            String result = auction.closeAuction(); // Gọi đóng phiên

            assertEquals(AuctionStatus.FINISHED, auction.getStatus());
            assertTrue(result.contains("Người chiến thắng"));
        });
    }

    // test đã đóng phiên nhưng vẫn gọi, ném exception
    @Test
    public void testCloseAuction_AlreadyClosed_ThrowsException() {
        auction.updateStatus(AuctionStatus.FINISHED);

        assertThrows(AuctionClosedException.class, () -> {
            auction.closeAuction();
        });
    }
}