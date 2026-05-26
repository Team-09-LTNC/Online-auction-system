package com.auction.server.manager;

import static org.assertj.core.api.Assertions.assertThat;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.AutoBidConfig;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.item.Electronics;
import com.auction.common.model.user.Bidder;
import com.auction.server.dao.AuctionDao;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuctionAutoBidServiceTest {
  @Test
  void highestAutoBidderWinsAgainstMultipleAutoBidders() {
    RecordingAuctionDao dao = new RecordingAuctionDao();
    Auction auction = runningAuction();
    Bidder bidderOne = bidder(1, "one");
    Bidder bidderTwo = bidder(2, "two");
    auction.addAutoBidConfig(new AutoBidConfig(bidderOne, 1_500L, 100L));
    auction.addAutoBidConfig(new AutoBidConfig(bidderTwo, 1_800L, 100L));

    AuctionAutoBidService service = newService(dao);
    service.triggerAutoBid(auction, null);

    assertThat(auction.getCurrentWinner()).isSameAs(bidderTwo);
    assertThat(auction.getCurrentHighestBid()).isEqualTo(1_600L);
    assertThat(dao.recordedBids).extracting(BidTransaction::getBidAmount).containsExactly(1_600L);
  }

  @Test
  void autoBidRespondsToSeveralManualBidsUntilMaxBidIsExceeded() {
    RecordingAuctionDao dao = new RecordingAuctionDao();
    Auction auction = runningAuction();
    Bidder autoBidder = bidder(2, "auto");
    Bidder manualBidder = bidder(3, "manual");
    auction.addAutoBidConfig(new AutoBidConfig(autoBidder, 1_800L, 100L));

    AuctionAutoBidService service = newService(dao);
    applyManualBid(auction, manualBidder, 1_200L);
    service.triggerAutoBid(auction, manualBidder.getId());
    applyManualBid(auction, manualBidder, 1_700L);
    service.triggerAutoBid(auction, manualBidder.getId());
    applyManualBid(auction, manualBidder, 1_900L);
    service.triggerAutoBid(auction, manualBidder.getId());

    assertThat(dao.recordedBids)
        .extracting(BidTransaction::getBidAmount)
        .containsExactly(1_300L, 1_800L);
    assertThat(auction.getCurrentWinner()).isSameAs(manualBidder);
    assertThat(auction.getCurrentHighestBid()).isEqualTo(1_900L);
  }

  private AuctionAutoBidService newService(RecordingAuctionDao dao) {
    return new AuctionAutoBidService(
        dao,
        auction -> {
        },
        (auctionId, tx) -> {
        },
        auction -> auction.setStatus(AuctionStatus.FINISHED));
  }

  private Auction runningAuction() {
    Electronics item = new Electronics("Laptop", "Gaming", 1_000L, "ASUS", 12);
    item.setId(10);
    item.setSellerId(99);
    item.setBidIncrement(100L);

    Auction auction = new Auction(item);
    auction.setId(100);
    auction.setStartTime(LocalDateTime.now().minusMinutes(1));
    auction.setEndTime(LocalDateTime.now().plusMinutes(30));
    auction.setStatus(AuctionStatus.RUNNING);
    return auction;
  }

  private Bidder bidder(int id, String username) {
    Bidder bidder = new Bidder(username, "pw", username);
    bidder.setId(id);
    return bidder;
  }

  private void applyManualBid(Auction auction, Bidder bidder, long amount) {
    auction.updateWinner(new BidTransaction(auction.getId(), bidder, amount, LocalDateTime.now()));
  }

  private static class RecordingAuctionDao extends AuctionDao {
    private final List<BidTransaction> recordedBids = new ArrayList<>();

    @Override
    public boolean executeBidTransaction(int auctionId, BidTransaction tx) {
      recordedBids.add(tx);
      return true;
    }
  }
}
