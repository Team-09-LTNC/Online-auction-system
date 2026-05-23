package com.auction.common.model.bid;

import java.time.LocalDateTime;
import java.util.*;

import com.auction.common.enums.AuctionStatus;
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

    // --- THÊM GIÁ MUA ĐỨT ---
    private Long buyNowPrice;

    private final List<BidTransaction> bidHistory = new ArrayList<>();
    private final Queue<AutoBidConfig> autoBidders = new PriorityQueue<>();

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

    public boolean isAcceptingBids() {
        return getStatus() == AuctionStatus.RUNNING;
    }

    public void addAutoBidConfig(AutoBidConfig config) { autoBidders.offer(config); }
    public Queue<AutoBidConfig> getAutoBidders() { return autoBidders; }

    public void extendEndTime(int extraSeconds) {
        this.endTime = this.endTime == null
                ? LocalDateTime.now().plusSeconds(extraSeconds)
                : this.endTime.plusSeconds(extraSeconds);
    }

    // --- Getters & Setters ---
    public Item getItem() { return item; }

    public long getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentPrice(long price) { this.currentHighestBid = price; }

    public Bidder getCurrentWinner() { return currentWinner; }
    public void setCurrentWinner(Bidder winner) { this.currentWinner = winner; }

    public AuctionStatus getStatus() {
        if (status == AuctionStatus.FINISHED
                || status == AuctionStatus.PAID
                || status == AuctionStatus.CANCELED) {
            return status;
        }

        LocalDateTime now = LocalDateTime.now();
        if (startTime != null && now.isBefore(startTime)) {
            return AuctionStatus.OPEN; // Chưa đến giờ bắt đầu
        }
        if (endTime != null && now.isAfter(endTime)) {
            return AuctionStatus.FINISHED; // Đã quá giờ kết thúc
        }
        return AuctionStatus.RUNNING; // Nằm trong khoảng thời gian diễn ra
    }
    public AuctionStatus getStoredStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public List<BidTransaction> getBidHistory() { return bidHistory; }

    // --- GETTER/SETTER CHO GIÁ MUA ĐỨT ---
    public Long getBuyNowPrice() { return buyNowPrice; }
    public void setBuyNowPrice(Long buyNowPrice) { this.buyNowPrice = buyNowPrice; }
}
