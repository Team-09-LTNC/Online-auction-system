package com.auction.server.manager;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.exception.AuctionClosedException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.AutoBidConfig;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.User;
import com.auction.server.dao.AuctionDao;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Queue;

final class AuctionBidCommandService {
  private final Map<Integer, Auction> runningAuctions;
  private final AuctionDao auctionDao;
  private final AuctionLifecycleService lifecycleService;
  private final AuctionPaymentTimeoutService paymentTimeoutService;
  private final AuctionAutoBidService autoBidService;
  private final AuctionRealtimeNotifier notifier;

  AuctionBidCommandService(
      Map<Integer, Auction> runningAuctions,
      AuctionDao auctionDao,
      AuctionLifecycleService lifecycleService,
      AuctionPaymentTimeoutService paymentTimeoutService,
      AuctionAutoBidService autoBidService,
      AuctionRealtimeNotifier notifier
  ) {
    this.runningAuctions = runningAuctions;
    this.auctionDao = auctionDao;
    this.lifecycleService = lifecycleService;
    this.paymentTimeoutService = paymentTimeoutService;
    this.autoBidService = autoBidService;
    this.notifier = notifier;
  }

  BidTransaction handleBuyNow(int auctionId, Bidder bidder)
      throws InvalidBidException, AuctionClosedException {
    Auction auction = runningAuctions.get(auctionId);
    if (auction == null) {
      throw new InvalidBidException("Phiên không khả dụng!");
    }

    synchronized (auction) {
      validateBuyNow(auction, bidder);
      long buyNowPrice = auction.getBuyNowPrice();
      BidTransaction transaction = new BidTransaction(auctionId, bidder, buyNowPrice, LocalDateTime.now());
      if (!auctionDao.executeBidTransaction(auctionId, transaction)) {
        throw new InvalidBidException("Đã có người trả giá cao hơn.");
      }

      auction.updateWinner(transaction);
      auction.setStatus(AuctionStatus.FINISHED);
      auctionDao.updateStatus(auctionId, AuctionStatus.FINISHED.name());
      lifecycleService.cancelAuctionCloseSchedule(auctionId);
      notifier.notifyNewBid(auctionId, transaction);
      notifier.notifyStatusChange(auctionId, AuctionStatus.FINISHED);
      AuctionDao.AuctionNotificationTargets targets =
          auctionDao.getAuctionEndNotificationTargets(auctionId);
      paymentTimeoutService.schedulePaymentTimeout(auctionId, targets);
      return transaction;
    }
  }

  void registerAutoBid(int auctionId, User bidder, long maxBid, long bidStep) throws Exception {
    Auction auction = runningAuctions.get(auctionId);
    if (auction == null) {
      throw new Exception("Phiên không khả dụng!");
    }

    synchronized (auction) {
      validateAutoBid(auction, bidder, maxBid, bidStep);
      Queue<AutoBidConfig> queue = auction.getAutoBidders();
      queue.removeIf(bot -> bot.getBidder().getId() == bidder.getId());
      auction.addAutoBidConfig(new AutoBidConfig(bidder, maxBid, bidStep));

      boolean saved = auctionDao.saveOrUpdateAutoBid(auctionId, bidder.getId(), maxBid, bidStep);
      if (!saved) {
        throw new Exception("Không thể lưu cấu hình Auto-bid. Vui lòng thử lại.");
      }
      autoBidService.triggerAutoBid(auction, null);
    }
  }

  void removeAutoBid(int auctionId, User bidder) throws Exception {
    Auction auction = runningAuctions.get(auctionId);
    if (auction == null) {
      throw new Exception("Phiên không khả dụng!");
    }
    synchronized (auction) {
      auction.getAutoBidders().removeIf(bot -> bot.getBidder().getId() == bidder.getId());
      if (!auctionDao.removeAutoBid(auctionId, bidder.getId())) {
        throw new Exception("Không thể xóa Auto-bid.");
      }
    }
  }

  private void validateBuyNow(Auction auction, Bidder bidder)
      throws InvalidBidException, AuctionClosedException {
    if (!auction.isAcceptingBids()) {
      lifecycleService.closeAuction(auction.getId());
      throw new AuctionClosedException("Phiên đã kết thúc!");
    }
    if (bidder.getId() == auction.getItem().getSellerId()) {
      throw new InvalidBidException("Không được mua sản phẩm của chính mình!");
    }
    if (auction.getBuyNowPrice() == null || auction.getBuyNowPrice() <= 0) {
      throw new InvalidBidException("Phiên này không hỗ trợ mua đứt.");
    }
    if (auction.getBuyNowPrice() <= auction.getCurrentHighestBid()) {
      throw new InvalidBidException("Giá mua đứt không còn hợp lệ ở thời điểm hiện tại.");
    }
  }

  private void validateAutoBid(Auction auction, User bidder, long maxBid, long bidStep) throws Exception {
    if (!auction.isAcceptingBids()) {
      lifecycleService.closeAuction(auction.getId());
      throw new AuctionClosedException("Phiên đã kết thúc!");
    }
    if (bidder.getId() == auction.getItem().getSellerId()) {
      throw new Exception("Seller không thể đăng ký Auto-bid cho sản phẩm của mình!");
    }
    long currentPrice = auction.getCurrentHighestBid();
    long sellerBidStep = auction.getItem().getBidIncrement();
    if (maxBid <= currentPrice) {
      throw new Exception("Giá max Auto-bid phải lớn hơn giá hiện tại.");
    }
    if (bidStep < sellerBidStep) {
      throw new Exception("Bước giá Auto-bid phải >= bước giá người bán.");
    }
  }
}
