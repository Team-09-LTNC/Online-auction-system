package com.auction.server.manager;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.bid.Auction;
import com.auction.server.dao.AuctionDao;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.Consumer;

final class AuctionAdminSyncService {
  private final AuctionDao auctionDao;
  private final Map<Integer, Auction> runningAuctions;
  private final ZoneId serverZone;
  private final Consumer<Auction> loadAutoBids;
  private final Consumer<Auction> scheduleStart;
  private final Consumer<Auction> scheduleClose;
  private final Consumer<Integer> cancelStart;
  private final Consumer<Integer> cancelClose;
  private final AuctionRealtimeNotifier notifier;

  AuctionAdminSyncService(
      AuctionDao auctionDao,
      Map<Integer, Auction> runningAuctions,
      ZoneId serverZone,
      Consumer<Auction> loadAutoBids,
      Consumer<Auction> scheduleStart,
      Consumer<Auction> scheduleClose,
      Consumer<Integer> cancelStart,
      Consumer<Integer> cancelClose,
      AuctionRealtimeNotifier notifier
  ) {
    this.auctionDao = auctionDao;
    this.runningAuctions = runningAuctions;
    this.serverZone = serverZone;
    this.loadAutoBids = loadAutoBids;
    this.scheduleStart = scheduleStart;
    this.scheduleClose = scheduleClose;
    this.cancelStart = cancelStart;
    this.cancelClose = cancelClose;
    this.notifier = notifier;
  }

  boolean approveAuctionSession(int auctionId) {
    Auction auction = auctionDao.getAuctionById(auctionId);
    if (auction == null) {
      return false;
    }

    String newStatus = resolveApprovedStatus(auction);
    if (!auctionDao.updateStatus(auctionId, newStatus)) {
      return false;
    }

    auction.setStatus(AuctionStatus.valueOf(newStatus));
    loadAutoBids.accept(auction);
    scheduleByStatus(auctionId, auction, newStatus);
    notifier.broadcastAuctionChanged(auctionId, "APPROVED", newStatus);
    return true;
  }

  void syncAfterStatusUpdate(int auctionId, String newStatus) {
    switch (newStatus) {
      case "CANCELED":
        cancelStart.accept(auctionId);
        cancelClose.accept(auctionId);
        notifier.notifyStatusChange(auctionId, AuctionStatus.CANCELED);
        break;
      case "OPEN":
        reloadOpenAuction(auctionId);
        break;
      case "RUNNING":
        reloadRunningAuction(auctionId);
        break;
      default:
        cancelStart.accept(auctionId);
        cancelClose.accept(auctionId);
        notifyTerminalStatus(auctionId, newStatus);
        break;
    }
  }

  void syncAfterAuctionDeleted(int auctionId) {
    cancelStart.accept(auctionId);
    cancelClose.accept(auctionId);
    runningAuctions.remove(auctionId);
    notifier.clearObservers(auctionId);
    notifier.broadcastAuctionChanged(auctionId, "DELETED", "DELETED");
  }

  private String resolveApprovedStatus(Auction auction) {
    LocalDateTime now = LocalDateTime.now(serverZone);
    if (now.isBefore(auction.getStartTime())) {
      return AuctionStatus.OPEN.name();
    }
    if (now.isAfter(auction.getEndTime())) {
      return AuctionStatus.CANCELED.name();
    }
    return AuctionStatus.RUNNING.name();
  }

  private void scheduleByStatus(int auctionId, Auction auction, String newStatus) {
    switch (newStatus) {
      case "OPEN":
        scheduleStart.accept(auction);
        break;
      case "RUNNING":
        runningAuctions.put(auctionId, auction);
        scheduleClose.accept(auction);
        break;
      default:
        break;
    }
  }

  private void reloadOpenAuction(int auctionId) {
    Auction auction = auctionDao.getAuctionById(auctionId);
    if (auction != null) {
      runningAuctions.remove(auctionId);
      loadAutoBids.accept(auction);
      scheduleStart.accept(auction);
      notifier.notifyStatusChange(auctionId, AuctionStatus.OPEN);
    }
  }

  private void reloadRunningAuction(int auctionId) {
    Auction auction = auctionDao.getAuctionById(auctionId);
    if (auction != null) {
      loadAutoBids.accept(auction);
      runningAuctions.put(auctionId, auction);
      scheduleClose.accept(auction);
      notifier.notifyStatusChange(auctionId, AuctionStatus.RUNNING);
    }
  }

  private void notifyTerminalStatus(int auctionId, String newStatus) {
    try {
      notifier.notifyStatusChange(auctionId, AuctionStatus.valueOf(newStatus));
    } catch (IllegalArgumentException ignored) {
      notifier.broadcastAuctionChanged(auctionId, "STATUS", newStatus);
    }
  }
}
