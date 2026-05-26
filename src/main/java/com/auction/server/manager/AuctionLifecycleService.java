package com.auction.server.manager;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.bid.Auction;
import com.auction.server.dao.AuctionDao;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AuctionLifecycleService {
  private static final Logger logger = LoggerFactory.getLogger(AuctionLifecycleService.class);

  private final AuctionDao auctionDao;
  private final Map<Integer, Auction> runningAuctions;
  private final Map<Integer, ScheduledFuture<?>> closeTasks;
  private final Map<Integer, ScheduledFuture<?>> startTasks;
  private final ScheduledExecutorService scheduler;
  private final AuctionPaymentTimeoutService paymentTimeoutService;
  private final AuctionRealtimeNotifier notifier;
  private final ZoneId serverZone;

  AuctionLifecycleService(
      AuctionDao auctionDao,
      Map<Integer, Auction> runningAuctions,
      Map<Integer, ScheduledFuture<?>> closeTasks,
      Map<Integer, ScheduledFuture<?>> startTasks,
      ScheduledExecutorService scheduler,
      AuctionPaymentTimeoutService paymentTimeoutService,
      AuctionRealtimeNotifier notifier,
      ZoneId serverZone
  ) {
    this.auctionDao = auctionDao;
    this.runningAuctions = runningAuctions;
    this.closeTasks = closeTasks;
    this.startTasks = startTasks;
    this.scheduler = scheduler;
    this.paymentTimeoutService = paymentTimeoutService;
    this.notifier = notifier;
    this.serverZone = serverZone;
  }

  void scheduleAuctionStart(Auction auction) {
    ScheduledFuture<?> oldTask = startTasks.get(auction.getId());
    if (oldTask != null && !oldTask.isDone()) {
      oldTask.cancel(false);
    }

    long delay = java.time.Duration.between(LocalDateTime.now(serverZone), auction.getStartTime()).toMillis();
    if (delay <= 0) {
      openAuction(auction);
      return;
    }

    ScheduledFuture<?> newTask = scheduler.schedule(() -> openAuction(auction), delay, TimeUnit.MILLISECONDS);
    startTasks.put(auction.getId(), newTask);
  }

  void scheduleAuctionClose(Auction auction) {
    ScheduledFuture<?> oldTask = closeTasks.get(auction.getId());
    if (oldTask != null && !oldTask.isDone()) {
      oldTask.cancel(false);
    }

    long delay = java.time.Duration.between(LocalDateTime.now(serverZone), auction.getEndTime()).toMillis();
    if (delay <= 0) {
      closeAuction(auction.getId());
      return;
    }

    ScheduledFuture<?> newTask =
        scheduler.schedule(() -> closeAuction(auction.getId()), delay, TimeUnit.MILLISECONDS);
    closeTasks.put(auction.getId(), newTask);
  }

  void closeAuction(int auctionId) {
    Auction auction = runningAuctions.remove(auctionId);
    if (auction == null) {
      return;
    }

    synchronized (auction) {
      AuctionDao.AuctionNotificationTargets targets =
          auctionDao.getAuctionEndNotificationTargets(auctionId);
      AuctionStatus newStatus = targets != null && targets.winnerId != null
          ? AuctionStatus.FINISHED
          : AuctionStatus.CANCELED;
      auction.setStatus(newStatus);
      auctionDao.updateStatus(auctionId, newStatus.name());

      if (newStatus == AuctionStatus.FINISHED) {
        logger.info("Dong phien {} FINISHED. winnerId={}, sellerId={}",
            auctionId,
            targets != null ? targets.winnerId : null,
            targets != null ? targets.sellerId : null);
        AuctionSettlementNotifier.sendAuctionEndNotification(targets);
        paymentTimeoutService.schedulePaymentTimeout(auctionId, targets);
      }
      notifier.notifyStatusChange(auctionId, newStatus);
      notifier.clearObservers(auctionId);
      closeTasks.remove(auctionId);
    }
  }

  void cancelAuctionCloseSchedule(int auctionId) {
    ScheduledFuture<?> closeTask = closeTasks.remove(auctionId);
    if (closeTask != null && !closeTask.isDone()) {
      closeTask.cancel(false);
    }
    runningAuctions.remove(auctionId);
  }

  void cancelAuctionStartSchedule(int auctionId) {
    ScheduledFuture<?> startTask = startTasks.remove(auctionId);
    if (startTask != null && !startTask.isDone()) {
      startTask.cancel(false);
    }
  }

  private void openAuction(Auction auction) {
    if (auctionDao.updateStatus(auction.getId(), AuctionStatus.RUNNING.name())) {
      auction.setStatus(AuctionStatus.RUNNING);
      runningAuctions.put(auction.getId(), auction);
      startTasks.remove(auction.getId());
      logger.info("Đã tự động mở phiên đấu giá ID: {}", auction.getId());
      scheduleAuctionClose(auction);
      notifier.notifyStatusChange(auction.getId(), AuctionStatus.RUNNING);
    }
  }
}
