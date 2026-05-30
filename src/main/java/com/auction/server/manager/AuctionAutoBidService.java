package com.auction.server.manager;

import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.AutoBidConfig;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.user.Bidder;
import com.auction.server.dao.AuctionDao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class AuctionAutoBidService {
    private final AuctionDao auctionDao;
    private final Consumer<Auction> extendIfLateBid;
    private final BiConsumer<Integer, BidTransaction> notifyNewBid;
    private final Consumer<Auction> finishAfterBuyNow;

    AuctionAutoBidService(
            AuctionDao auctionDao,
            Consumer<Auction> extendIfLateBid,
            BiConsumer<Integer, BidTransaction> notifyNewBid,
            Consumer<Auction> finishAfterBuyNow
    ) {
        this.auctionDao = auctionDao;
        this.extendIfLateBid = extendIfLateBid;
        this.notifyNewBid = notifyNewBid;
        this.finishAfterBuyNow = finishAfterBuyNow;
    }

    void triggerAutoBid(Auction phien, Integer manualBidderId) {
        if (!phien.isAcceptingBids()) {
            return;
        }

        List<AutoBidConfig> activeBots = getActiveAutoBids(phien);
        if (activeBots.isEmpty()) {
            return;
        }

        AutoBidConfig leader = activeBots.get(0);
        AutoBidConfig challenger = findNextCompetitor(activeBots, leader);
        if (isCurrentWinner(phien, leader) || isLastBidder(phien, leader)) {
            return;
        }

        long nextBidAmount = calculateAutoBidAmount(
                phien,
                leader,
                challenger,
                isManualCurrentWinner(phien, leader, manualBidderId));
        long minimumNextBid = phien.getCurrentHighestBid() + phien.getItem().getBidIncrement();
        Long buyNowPrice = phien.getBuyNowPrice();
        boolean reachedBuyNow = buyNowPrice != null
                && buyNowPrice > 0
                && phien.getCurrentHighestBid() < buyNowPrice
                && nextBidAmount >= buyNowPrice
                && leader.getMaxBid() >= buyNowPrice;

        if (!reachedBuyNow && (nextBidAmount < minimumNextBid || nextBidAmount > leader.getMaxBid())) {
            return;
        }

        if (reachedBuyNow) {
            nextBidAmount = buyNowPrice;
        }

        BidTransaction autoTx = new BidTransaction(
                phien.getId(),
                (Bidder) leader.getBidder(),
                nextBidAmount,
                LocalDateTime.now());

        if (!auctionDao.executeBidTransaction(phien.getId(), autoTx)) {
            return;
        }

        phien.updateWinner(autoTx);
        extendIfLateBid.accept(phien);
        notifyNewBid.accept(phien.getId(), autoTx);

        if (reachedBuyNow) {
            finishAfterBuyNow.accept(phien);
        }
    }

    private List<AutoBidConfig> getActiveAutoBids(Auction phien) {
        long currentPrice = phien.getCurrentHighestBid();
        List<AutoBidConfig> activeBots = new ArrayList<>();
        for (AutoBidConfig bot : phien.getAutoBidders()) {
            if (bot.getMaxBid() > currentPrice) {
                activeBots.add(bot);
            }
        }
        Collections.sort(activeBots);
        return activeBots;
    }

    private AutoBidConfig findNextCompetitor(List<AutoBidConfig> activeBots, AutoBidConfig leader) {
        for (AutoBidConfig bot : activeBots) {
            if (bot.getBidder().getId() != leader.getBidder().getId()) {
                return bot;
            }
        }
        return null;
    }

    private long calculateAutoBidAmount(
            Auction phien,
            AutoBidConfig leader,
            AutoBidConfig challenger,
            boolean currentWinnerIsManualBidder
    ) {
        long currentPrice = phien.getCurrentHighestBid();
        long minimumNextBid = currentPrice + phien.getItem().getBidIncrement();
        long targetBid;
        if (currentWinnerIsManualBidder) {
            targetBid = currentPrice + effectiveAutoBidStep(phien, leader);
            return targetBid;
        } else if (challenger == null) {
            targetBid = currentPrice + effectiveAutoBidStep(phien, leader);
        } else if (leader.getMaxBid() == challenger.getMaxBid()) {
            targetBid = currentPrice + effectiveAutoBidStep(phien, leader);
        } else {
            targetBid = challenger.getMaxBid() + phien.getItem().getBidIncrement();
        }
        return Math.max(targetBid, minimumNextBid);
    }

    private boolean isManualCurrentWinner(
            Auction phien,
            AutoBidConfig leader,
            Integer manualBidderId
    ) {
        return manualBidderId != null
                && phien.getCurrentWinner() != null
                && phien.getCurrentWinner().getId() == manualBidderId
                && leader.getBidder().getId() != manualBidderId;
    }

    private long effectiveAutoBidStep(Auction phien, AutoBidConfig bot) {
        return Math.max(bot.getBidStep(), phien.getItem().getBidIncrement());
    }

    private boolean isCurrentWinner(Auction phien, AutoBidConfig bot) {
        return phien.getCurrentWinner() != null
                && phien.getCurrentWinner().getId() == bot.getBidder().getId();
    }

    private boolean isLastBidder(Auction phien, AutoBidConfig bot) {
        List<BidTransaction> bidHistory = phien.getBidHistory();
        if (bidHistory.isEmpty()) {
            return false;
        }
        BidTransaction latestBid = bidHistory.get(bidHistory.size() - 1);
        return latestBid.getBidder() != null
                && latestBid.getBidder().getId() == bot.getBidder().getId();
    }
}
