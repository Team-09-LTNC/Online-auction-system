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
  void higherAutoBidderBeatsChallengerBySellerIncrementOnly() {
    RecordingAuctionDao dao = new RecordingAuctionDao();
    Auction auction = runningAuction();
    auction.getItem().setBidIncrement(20_000L);
    Bidder bidderA = bidder(1, "a");
    Bidder bidderB = bidder(2, "b");
    auction.addAutoBidConfig(new AutoBidConfig(bidderA, 7_000_000L, 30_000L));
    auction.addAutoBidConfig(new AutoBidConfig(bidderB, 6_000_000L, 40_000L));

    AuctionAutoBidService service = newService(dao);
    service.triggerAutoBid(auction, null);

    assertThat(auction.getCurrentWinner()).isSameAs(bidderA);
    assertThat(auction.getCurrentHighestBid()).isEqualTo(6_020_000L);
    assertThat(dao.recordedBids).extracting(BidTransaction::getBidAmount).containsExactly(6_020_000L);
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

  @Test
  void equalMaxAutoBidDoesNotRaisePriceWhenEarlierBidderAlreadyLeads() {
    RecordingAuctionDao dao = new RecordingAuctionDao();
    Auction auction = runningAuction();
    Bidder earlyBidder = bidder(2, "early");
    Bidder lateBidder = bidder(3, "late");
    LocalDateTime earlyTime = LocalDateTime.of(2026, 5, 30, 10, 0);
    auction.addAutoBidConfig(new AutoBidConfig(earlyBidder, 2_000L, 100L, earlyTime));
    auction.addAutoBidConfig(new AutoBidConfig(lateBidder, 2_000L, 100L, earlyTime.plusMinutes(1)));
    applyManualBid(auction, earlyBidder, 1_100L);

    AuctionAutoBidService service = newService(dao);
    service.triggerAutoBid(auction, null);

    assertThat(dao.recordedBids).isEmpty();
    assertThat(auction.getCurrentWinner()).isSameAs(earlyBidder);
    assertThat(auction.getCurrentHighestBid()).isEqualTo(1_100L);
  }

  @Test
  void equalMaxAutoBidCreatesAtMostOneBidWhenNoBidderLeadsYet() {
    RecordingAuctionDao dao = new RecordingAuctionDao();
    Auction auction = runningAuction();
    Bidder earlyBidder = bidder(2, "early");
    Bidder lateBidder = bidder(3, "late");
    LocalDateTime earlyTime = LocalDateTime.of(2026, 5, 30, 10, 0);
    auction.addAutoBidConfig(new AutoBidConfig(earlyBidder, 2_000L, 100L, earlyTime));
    auction.addAutoBidConfig(new AutoBidConfig(lateBidder, 2_000L, 100L, earlyTime.plusMinutes(1)));

    AuctionAutoBidService service = newService(dao);
    service.triggerAutoBid(auction, null);

    assertThat(dao.recordedBids).extracting(BidTransaction::getBidAmount).containsExactly(1_100L);
    assertThat(auction.getCurrentWinner()).isSameAs(earlyBidder);
    assertThat(auction.getCurrentHighestBid()).isEqualTo(1_100L);
  }

  @Test
  void equalMaxAutoBidDoesNotClimbToMaxWhenTriggeredRepeatedly() {
    RecordingAuctionDao dao = new RecordingAuctionDao();
    Auction auction = runningAuction();
    Bidder earlyBidder = bidder(2, "early");
    Bidder lateBidder = bidder(3, "late");
    LocalDateTime earlyTime = LocalDateTime.of(2026, 5, 30, 10, 0);
    auction.addAutoBidConfig(new AutoBidConfig(earlyBidder, 90_000L, 1_000L, earlyTime));
    auction.addAutoBidConfig(new AutoBidConfig(lateBidder, 90_000L, 1_000L, earlyTime.plusMinutes(1)));

    AuctionAutoBidService service = newService(dao);
    for (int i = 0; i < 20; i++) {
      service.triggerAutoBid(auction, null);
    }

    assertThat(dao.recordedBids).extracting(BidTransaction::getBidAmount).containsExactly(2_000L);
    assertThat(auction.getCurrentWinner()).isSameAs(earlyBidder);
    assertThat(auction.getCurrentHighestBid()).isEqualTo(2_000L);
  }

  @Test
  void equalMaxAutoBidRespondsOnlyOnceWhenLaterBidderTemporarilyLeads() {
    RecordingAuctionDao dao = new RecordingAuctionDao();
    Auction auction = runningAuction();
    Bidder earlyBidder = bidder(2, "early");
    Bidder lateBidder = bidder(3, "late");
    LocalDateTime earlyTime = LocalDateTime.of(2026, 5, 30, 10, 0);
    auction.addAutoBidConfig(new AutoBidConfig(earlyBidder, 2_000L, 100L, earlyTime));
    auction.addAutoBidConfig(new AutoBidConfig(lateBidder, 2_000L, 100L, earlyTime.plusMinutes(1)));
    applyManualBid(auction, lateBidder, 1_500L);

    AuctionAutoBidService service = newService(dao);
    service.triggerAutoBid(auction, lateBidder.getId());

    assertThat(dao.recordedBids).extracting(BidTransaction::getBidAmount).containsExactly(1_600L);
    assertThat(auction.getCurrentWinner()).isSameAs(earlyBidder);
    assertThat(auction.getCurrentHighestBid()).isEqualTo(1_600L);
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
