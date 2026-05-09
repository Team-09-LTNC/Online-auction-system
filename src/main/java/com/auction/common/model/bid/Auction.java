package com.auction.common.model.bid;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.auction.common.model.AuctionStatus;
import com.auction.common.model.entity.Entity;
import com.auction.common.model.item.Item;
import com.auction.common.model.user.Bidder;

public class Auction extends Entity {
    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private long currentHighestBid;
    private Bidder currentWinner;

    private final List<BidTransaction> bidHistory = new ArrayList<>();

    public Auction(Item item) {
        this.item = item;
        this.currentHighestBid = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
    }

    public void updateWinner(BidTransaction transaction) {
        this.bidHistory.add(transaction);
        this.currentHighestBid = transaction.getBidAmount();
        this.currentWinner = transaction.getBidder();
    }

    // Getters & Setters
    public Item getItem() { return item; }
    public long getCurrentHighestBid() { return currentHighestBid; }
    public Bidder getCurrentWinner() { return currentWinner; }
    public void setCurrentWinner(Bidder winner) { this.currentWinner = winner; }
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
}