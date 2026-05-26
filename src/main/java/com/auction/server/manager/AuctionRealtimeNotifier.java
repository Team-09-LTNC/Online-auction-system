package com.auction.server.manager;

import com.auction.common.enums.ActionType;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.observer.AuctionObserver;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

final class AuctionRealtimeNotifier {
  private final Map<Integer, List<AuctionObserver>> observersByAuction;
  private final Map<Integer, Auction> runningAuctions;
  private final ExecutorService notifierPool;
  private final ZoneId serverZone;

  AuctionRealtimeNotifier(
      Map<Integer, List<AuctionObserver>> observersByAuction,
      Map<Integer, Auction> runningAuctions,
      ExecutorService notifierPool,
      ZoneId serverZone
  ) {
    this.observersByAuction = observersByAuction;
    this.runningAuctions = runningAuctions;
    this.notifierPool = notifierPool;
    this.serverZone = serverZone;
  }

  void subscribe(int auctionId, AuctionObserver observer) {
    List<AuctionObserver> observers =
        observersByAuction.computeIfAbsent(auctionId, key -> new CopyOnWriteArrayList<>());
    if (!observers.contains(observer)) {
      observers.add(observer);
    }
  }

  void unsubscribe(int auctionId, AuctionObserver observer) {
    List<AuctionObserver> observers = observersByAuction.get(auctionId);
    if (observers != null) {
      observers.removeIf(current -> current == observer);
    }
  }

  void notifyNewBid(int auctionId, BidTransaction transaction) {
    List<AuctionObserver> observers = observersByAuction.get(auctionId);
    if (observers != null) {
      for (AuctionObserver observer : observers) {
        observer.onNewBid(transaction);
      }
    }
    Auction auction = runningAuctions.get(auctionId);
    broadcastAuctionChanged(
        auctionId,
        "BID",
        auction != null && auction.getStatus() != null ? auction.getStatus().name() : null);
  }

  void notifyStatusChange(int auctionId, AuctionStatus status) {
    List<AuctionObserver> observers = observersByAuction.get(auctionId);
    if (observers != null) {
      for (AuctionObserver observer : observers) {
        observer.onStatusChanged(status);
      }
    }
    broadcastAuctionChanged(auctionId, "STATUS", status != null ? status.name() : null);
  }

  void broadcastAuctionChanged(int auctionId, String reason, String status) {
    JsonObject payload = new JsonObject();
    payload.addProperty("type", ActionType.AUCTION_CHANGED);
    payload.addProperty("auctionId", auctionId);
    payload.addProperty("reason", reason != null ? reason : "UNKNOWN");
    if (status != null) {
      payload.addProperty("status", status);
    }
    payload.addProperty("serverNow", LocalDateTime.now(serverZone).toString());
    UserManager.getInstance().broadcastPushEvent(payload);
  }

  void broadcastChatMessage(int auctionId, String senderName, String message, boolean isSystem) {
    notifierPool.execute(() -> {
      List<AuctionObserver> observers = observersByAuction.get(auctionId);
      if (observers != null) {
        for (AuctionObserver observer : observers) {
          observer.onChatMessage(senderName, message, isSystem);
        }
      }
    });
  }

  void clearObservers(int auctionId) {
    observersByAuction.remove(auctionId);
  }
}
